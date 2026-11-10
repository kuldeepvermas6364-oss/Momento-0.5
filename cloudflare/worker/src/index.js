export default {
  async fetch(request, env) {
    const cors = {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type, Authorization",
      "Content-Type": "application/json"
    };
    if (request.method === "OPTIONS") return new Response(null, { headers: cors });
    if (request.method !== "POST") return reply({ error: "Method not allowed" }, 405, cors);

    try {
      const auth = request.headers.get("Authorization") || "";
      const idToken = auth.startsWith("Bearer ") ? auth.slice(7) : "";
      if (!idToken) return reply({ error: "Firebase sign-in required" }, 401, cors);
      if (!env.FIREBASE_WEB_API_KEY || !env.CLOUDINARY_API_KEY ||
          !env.CLOUDINARY_API_SECRET || !env.CLOUDINARY_CLOUD_NAME) {
        return reply({ error: "Upload service is not configured" }, 503, cors);
      }

      // Validate the Firebase ID token with Firebase Authentication before signing.
      const verifyResponse = await fetch(
        `https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=${env.FIREBASE_WEB_API_KEY}`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ idToken })
        }
      );
      if (!verifyResponse.ok) return reply({ error: "Invalid or expired Firebase session" }, 401, cors);
      const verified = await verifyResponse.json();
      const uid = verified.users?.[0]?.localId;
      if (!uid) return reply({ error: "Unable to verify account" }, 401, cors);

      const body = await request.json();
      const resourceType = body.resourceType === "video" ? "video" :
        body.resourceType === "image" ? "image" : null;
      if (!resourceType) return reply({ error: "Only image and video uploads are enabled" }, 400, cors);

      const timestamp = Math.floor(Date.now() / 1000).toString();
      const folder = `momento/${uid}`;
      const toSign = `folder=${folder}&timestamp=${timestamp}`;
      const signature = await sha1Hex(toSign + env.CLOUDINARY_API_SECRET);

      return reply({
        cloudName: env.CLOUDINARY_CLOUD_NAME,
        apiKey: env.CLOUDINARY_API_KEY,
        timestamp,
        folder,
        signature,
        resourceType
      }, 200, cors);
    } catch (error) {
      return reply({ error: "Unable to create upload signature" }, 500, cors);
    }
  }
};

async function sha1Hex(value) {
  const bytes = new TextEncoder().encode(value);
  const digest = await crypto.subtle.digest("SHA-1", bytes);
  return [...new Uint8Array(digest)].map(b => b.toString(16).padStart(2, "0")).join("");
}

function reply(data, status, headers) {
  return new Response(JSON.stringify(data), { status, headers });
}
