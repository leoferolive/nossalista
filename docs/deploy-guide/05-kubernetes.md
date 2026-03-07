# Kubernetes — Manifests

## 1. Reorganização da Pasta k8s/

```
k8s/
├── dev/
│   ├── namespace.yaml
│   ├── deployment.yaml
│   ├── service.yaml
│   └── ingress.yaml
└── prod/
    ├── namespace.yaml
    ├── deployment.yaml
    ├── service.yaml
    └── ingress.yaml
```

Os arquivos na raiz `k8s/` (manifests antigos) podem ser removidos ou arquivados após os novos estarem validados.

---

## 2. k8s/dev/namespace.yaml

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: nossalista-dev
```

---

## 3. k8s/dev/deployment.yaml

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nossalista-dev
  namespace: nossalista-dev
spec:
  replicas: 1
  selector:
    matchLabels:
      app: nossalista-dev
  template:
    metadata:
      labels:
        app: nossalista-dev
    spec:
      imagePullSecrets:
        - name: ghcr-secret
      containers:
        - name: nossalista-dev
          image: ghcr.io/leoferolive/nossalista:dev
          imagePullPolicy: Always
          ports:
            - containerPort: 8080
          envFrom:
            - secretRef:
                name: nossalista-secrets
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "1000m"
          startupProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 18
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            periodSeconds: 5
            timeoutSeconds: 5
            failureThreshold: 3
```

---

## 4. k8s/dev/service.yaml

```yaml
apiVersion: v1
kind: Service
metadata:
  name: nossalista-dev
  namespace: nossalista-dev
spec:
  selector:
    app: nossalista-dev
  ports:
    - port: 80
      targetPort: 8080
  type: ClusterIP
```

---

## 5. k8s/dev/ingress.yaml

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: nossalista-dev
  namespace: nossalista-dev
  annotations:
    traefik.ingress.kubernetes.io/router.entrypoints: web
spec:
  ingressClassName: traefik
  rules:
    - host: nossalista.home
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: nossalista-dev
                port:
                  number: 80
```

---

## 6. k8s/prod/namespace.yaml

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: nossalista
```

---

## 7. k8s/prod/deployment.yaml

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nossalista
  namespace: nossalista
spec:
  replicas: 1
  selector:
    matchLabels:
      app: nossalista
  template:
    metadata:
      labels:
        app: nossalista
    spec:
      imagePullSecrets:
        - name: ghcr-secret
      containers:
        - name: nossalista
          image: ghcr.io/leoferolive/nossalista:latest
          imagePullPolicy: Always
          ports:
            - containerPort: 8080
          envFrom:
            - secretRef:
                name: nossalista-secrets
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: prod
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "1000m"
          startupProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 18
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            periodSeconds: 5
            timeoutSeconds: 5
            failureThreshold: 3
```

---

## 8. k8s/prod/service.yaml

```yaml
apiVersion: v1
kind: Service
metadata:
  name: nossalista
  namespace: nossalista
spec:
  selector:
    app: nossalista
  ports:
    - port: 80
      targetPort: 8080
  type: ClusterIP
```

---

## 9. k8s/prod/ingress.yaml

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: nossalista
  namespace: nossalista
  annotations:
    traefik.ingress.kubernetes.io/router.entrypoints: web
spec:
  ingressClassName: traefik
  rules:
    - host: nossalista.leoferolive.com.br
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: nossalista
                port:
                  number: 80
```

> **Nota:** TLS não é configurado no Ingress — o Cloudflare Tunnel termina TLS externamente. O tráfego interno cluster é HTTP.

---

## 10. WebSocket — Traefik

Traefik 2+ propaga automaticamente o header `Upgrade: websocket`. Nenhuma anotação adicional é necessária para que STOMP/SockJS funcione através do Ingress.

Se houver problemas de timeout em conexões WebSocket longas, adicionar:

```yaml
# No ingress.yaml (opcional)
annotations:
  traefik.ingress.kubernetes.io/router.entrypoints: web
  traefik.ingress.kubernetes.io/router.middlewares: default-ws-headers@kubernetescrd
```

---

## 11. Comandos de Aplicação

```bash
# Aplicar ambiente dev
kubectl apply -f k8s/dev/

# Aplicar ambiente prod
kubectl apply -f k8s/prod/

# Verificar status
kubectl get pods -n nossalista-dev
kubectl get pods -n nossalista

# Ver logs
kubectl logs -f deployment/nossalista-dev -n nossalista-dev
kubectl logs -f deployment/nossalista -n nossalista

# Restart manual (força novo pull da imagem)
kubectl rollout restart deployment/nossalista-dev -n nossalista-dev
kubectl rollout restart deployment/nossalista -n nossalista

# Verificar ingress
kubectl get ingress -A
```
