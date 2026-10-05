const nextConfig = {
  output: "export",
  images: { unoptimized: true },
  trailingSlash: true,
  ...(process.env.NODE_ENV === "development"
    ? {
        async rewrites() {
          return [
            {
              source: "/api/:path*",
              destination: "http://127.0.0.1:4178/api/:path*",
            },
          ];
        },
      }
    : {}),
};
export default nextConfig;
