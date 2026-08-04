import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  /**
   * Only the Dockerfile sets this. A standalone build traces what the server
   * actually imports and writes a server.js beside it, which is what keeps the
   * image to that instead of a copy of node_modules — but it also replaces
   * `next start` with `node server.js`, so leaving it off here keeps the local
   * commands in the README working as they are documented.
   */
  output: process.env.NEXT_OUTPUT === "standalone" ? "standalone" : undefined,

  experimental: {
    serverActions: {
      /**
       * Photos reach the API through a server action, so this is the narrowest
       * gate they pass. The default is 1 MB, which silently contradicted every
       * other limit in the project — the backend accepts a 5 MB file, and the
       * forms say so — and a 2 MB photo failed with an unhandled runtime error
       * rather than a message.
       *
       * 6 MB rather than 5: the limit covers the whole multipart body, so the
       * boundaries, part headers and the other form fields have to fit beside
       * the file. Still under the backend's own 10 MB request cap, which stays
       * the real ceiling.
       */
      bodySizeLimit: "6mb",
    },
  },
};

export default nextConfig;
