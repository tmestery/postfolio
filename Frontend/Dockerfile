# Dev server image (hot reload). Build from repo root via compose.
FROM node:22-alpine
WORKDIR /app
COPY Frontend/package.json Frontend/package-lock.json* ./
RUN npm install
COPY Frontend/ ./
EXPOSE 5173
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0", "--port", "5173"]
