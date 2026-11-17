FROM node:22-bookworm-slim
WORKDIR /srv/asgram-review
COPY package.json package-lock.json ./
RUN npm ci --omit=dev --ignore-scripts
COPY server.mjs ./
USER node
ENV NODE_ENV=production BIND_HOST=0.0.0.0 PORT=8098
EXPOSE 8098
CMD ["node", "server.mjs"]
