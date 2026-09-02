FROM node:20-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# This is a public service URL, not a credential. The deployment workflow sets it
# explicitly so the generated bundle always targets the production API.
ARG VITE_API_BASE_URL=https://scan-pilot-api-drbjfwrlxq-as.a.run.app
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

RUN npm run build

FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 8080

CMD ["nginx", "-g", "daemon off;"]
