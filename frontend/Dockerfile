# ==========================================
# Stage 1: Build React Production Bundle
# ==========================================
FROM node:20-alpine AS builder

WORKDIR /app

# Cache package dependencies
COPY package.json ./
RUN npm install

# Copy source and build static bundle
COPY . .
RUN npm run build

# ==========================================
# Stage 2: Nginx Edge Web Server & Reverse Proxy
# ==========================================
FROM nginx:1.31-alpine AS runtime

# Copy custom Nginx reverse proxy configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy compiled SPA bundle from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost/healthz || exit 1

CMD ["nginx", "-g", "daemon off;"]
