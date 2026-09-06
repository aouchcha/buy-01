# Audit Backend — buy-01

**Date** : 2026-08-30 | **Branche** : buy-02 | **Périmètre** : `Backend/` (7 microservices Spring Boot : discovery, gateway, user, product, orders, media, search) + infra (docker-compose, Jenkinsfile)
**Méthode** : 3 agents en parallèle (code quality, sécurité, devops/infra), lecture seule, aucun fichier modifié. Projet non enregistré dans le hub — audit ad-hoc, pas de suivi historique.

---

## Résumé exécutif

| Axe | Critique | Élevée | Moyenne | Faible |
|---|---|---|---|---|
| Code / Correction | 3 | — | 7 (regroupés "Important") | 6 (regroupés "Mineur") |
| Sécurité | 2 | 2 | 3 | 5 |
| DevOps / Infra | 5 | 4 | 4 | 2 |

**Les 3 problèmes à traiter en premier, tous vérifiés par lecture directe du code/config :**

1. **Secret JWT et mot de passe MongoDB réels commités dans `.env.example`**, présents dans l'historique git du dépôt GitHub (`aouchcha/buy-01`), commit HEAD `1cc3b4e`. Un secret déjà réintroduit après une première "correction" (`ad2b22b`).
2. **`OrderService.java:122`** : condition `&&` au lieu de `||` → la garde censée bloquer la suppression de commandes livrées/expédiées ne se déclenche **jamais** → suppression de commandes livrées possible sans restriction.
3. **`docker-compose.jenkins.yml` référencé par le Jenkinsfile mais absent du repo** → la CI casse dès le 2ᵉ stage. En l'état, le pipeline ne peut pas tourner.

---

## 1. Audit Code / Correction

### Vue d'ensemble
- Spring Boot **4.0.6**, Java 21 partout — cohérent au niveau parent.
- `spring-cloud.version` divergent (2025.1.1 / 2025.1.3 / absent) et `spring-boot-starter-validation` épinglé en **RC (4.1.0-RC1)** sur 4 services sur 6.
- `search/pom.xml:40` déclare `spring-boot-starter-kafka`, artefact qui n'existe pas dans l'écosystème officiel (l'intégration se fait via `spring-kafka`) — **[NON VÉRIFIÉ, build Maven impossible dans cet environnement]**, à confirmer avec `mvn dependency:tree`.
- 57 `System.out`/`System.err` contre seulement 4 fichiers utilisant SLF4J — aucune stratégie de logging.
- Tests absents sur discovery, gateway, orders, search — dont orders (logique métier) et gateway (sécurité), les deux services les plus sensibles.
- Conventions de nommage Java violées massivement dans `user` (13 classes en minuscule : `userRepository.java`, `usersController.java`, etc.).

### Findings critiques
- **`gateway/src/main/resources/application.properties:39-45`** : `routes[5]` défini deux fois (cart-service puis search-service) — la seconde écrase la première. La route `/api/cart/**` est très probablement non fonctionnelle.
- **`orders/.../OrderService.java:122`** : `if (status == DELIVERED && status == SHIPPED)` — toujours faux, la garde ne protège jamais rien. Bug fonctionnel réel, à corriger en `||`.
- **`search/pom.xml:40`** : artefact `spring-boot-starter-kafka` suspect, à vérifier avant tout déploiement.

### Findings importants (sélection — détail complet dans le rapport agent)
- `media/.../MediaService.java:100,211,246` : `throw new InternalError(...)` résout vers `java.lang.InternalError` du JDK (import manquant de la classe custom) — la classe métier `MyExeptions/InternalError` n'est jamais réellement levée. Confirmé par un commentaire dans `MediaServiceTest.java:184-199` qui documente déjà ce bug.
- `orders/.../CartService.getCart` : `catch (Exception e) { item.setOutOfStock(true); }` avale toutes les exceptions (réseau, 500, timeout) et les traduit silencieusement en "rupture de stock" — masque de vrais incidents.
- `orders/.../RestTemplateConfig.java:22` : aucun timeout configuré → un product-service lent/down peut bloquer les threads d'orders indéfiniment.
- `search` : aucun `GlobalExceptionHandler`, seul service à exposer directement l'entité de persistance sans DTO ni `ResponseEntity`.
- Gestion d'erreurs incohérente d'un service à l'autre (catch-all présent seulement dans product), formats de réponse hétérogènes, codes delete incohérents (204 vs 200).
- Duplication exacte de la classe `Jwt` entre `user` et `gateway` — aucune librairie partagée.

### Findings mineurs
Code mort (`gateway/Security/JwtAuthFilter.java` entièrement commenté), TODOs obsolètes, typos (`unauthrizedHandler`, `MyForbiddenHandelr`, "Lunched"), logs de debug oubliés (média, product, orders, gateway).

---

## 2. Audit Sécurité

### CRITIQUE

**C1 — Secrets réels dans `.env.example` (tracké git, poussé sur GitHub)**
- `DB_PASSWORD=Achraf1303`, `JWT_SECRET=AchrafOUCHCHATE1303AchrafOUCHCHATE1303AchrafOUCHCHATE1303`, URIs MongoDB avec identifiants en clair, `SSL_KEYSTORE_PASSWORD=buy01pass`.
- Confirmé présent au commit HEAD actuel, après avoir déjà été remplacé par des placeholders dans un commit antérieur puis réintroduit.
- **Exploitation** : avec le secret JWT, un attaquant forge lui-même un token signé `role=ADMIN`, sans connaître aucun mot de passe utilisateur.
- **[NON VÉRIFIÉ]** si ce mot de passe est réellement réutilisé en prod/staging — mais le risque doit être traité comme réel tant que ce n'est pas infirmé.

**C2 — Confiance aveugle dans les en-têtes `X-User-Id`/`X-User-Role` par tous les services en aval**
- Seule la gateway valide le JWT (`JwtFilter.java:84-114`) puis injecte ces en-têtes. Aucun service en aval (user, product, media, orders) ne revérifie leur authenticité — ni HMAC, ni mTLS. Seule barrière : l'isolation réseau Docker.
- **Exploitation** : un accès au réseau interne (conteneur compromis, SSRF) permet d'envoyer directement `X-User-Role: ADMIN` à `user-service` pour un `DELETE /api/users/{id}` — élévation de privilèges complète sans JWT.

### ÉLEVÉE
- **E1** : Aucun rate limiting sur la gateway, y compris `/api/auth/**` — brute force/credential stuffing illimité sur le login.
- **E2** : Elasticsearch déployé avec `xpack.security.enabled=false` (`docker-compose.yml:160`) — aucune authentification sur l'index produit au sein du réseau interne.

### MOYENNE
- Bean CORS de la gateway probablement inopérant (`.cors(disable())` + bean jamais câblé à un `CorsWebFilter`) — à confirmer par un test `OPTIONS` réel.
- Logs de debug verbeux dans `JwtFilter` à chaque requête (host, origin, URL complète).
- `logging.level.org.springframework.security=DEBUG` actif en permanence sur `user` (pas de profil dédié).

### FAIBLE
- IDOR sur `GET /api/media/{id}` (pas de vérification de propriétaire, contrairement à `getAll`/`deleteMedia`).
- `management.endpoint.health.show-details=always` exposé sans authentification sur les 7 services (accessible seulement depuis le réseau interne).
- `spring-boot-starter-validation` en RC dans un module de prod (`user/pom.xml:52`).
- Pas de mécanisme de refresh/révocation de JWT (limite classique du stateless, JWT expire en 1h).

### Points vérifiés sans problème
Mots de passe hashés BCrypt, requêtes Elasticsearch/MongoDB paramétrées (pas d'injection), upload média validé par contenu réel (Apache Tika) avec noms de fichiers en UUID (pas de path traversal), versions Spring Boot/jjwt à jour sans CVE connue.

---

## 3. Audit DevOps / Infra

### CRITIQUE
1. **`docker-compose.jenkins.yml` référencé (`Jenkinsfile:210,249`, `scripts/detect-changed-services.sh:16`, `scripts/check.sh:7`) mais absent du repo** — jamais commité. La CI casse dès le 2ᵉ stage.
2. Secrets réels dans `.env.example` (cf. section sécurité C1) — confirmé aussi côté infra par contraste avec `AWS_ACCESS_KEY_ID=changeme` qui est un vrai placeholder.
3. **`Backend/orders/dockerfile`** en minuscule (tous les autres services : `Dockerfile`) — build cassé sur tout hôte Linux sensible à la casse (ne fonctionne que par accident sur macOS).
4. **Service `orders` absent de la commande de déploiement Jenkins** (`Jenkinsfile:251` ne le liste pas) — jamais démarré/mis à jour par la CI.
5. **Tests/quality gate contournables pour `orders`/`search`** : ces services sont absents des conditions `when` des stages de test/SonarQube, mais la stage `Build Docker Images` s'exécute quand même si eux seuls changent → image buildée sans tests.

### ÉLEVÉE
6. Microservice `search` non orchestré : pas de Dockerfile, absent des 3 fichiers docker-compose, absent du Jenkinsfile — l'ajout d'Elasticsearch n'a pas de consommateur applicatif dans l'infra.
7. `.env.example` incomplet vs variables réellement utilisées : `ORDERS_DB_URI`, `MEDIA_DB_NAME`, `ORDERS_DB_NAME`, `SONARQUBE_DB_*`, `USER_ID` manquants (services démarrent avec config vide/invalide) ; à l'inverse `GITHUB_TOKEN`/`NGROK_TOKEN`/`JENKINS_ADMIN_*`/`SMTP_*` définis mais inutilisés.
8. Drift de nommage : réseau toujours `buy01-network` malgré le renommage `COMPOSE_PROJECT_NAME` → `buy02` ; `scripts/check.sh` cherche encore des containers `buy-01-*` — cassé silencieusement par le fix récent.
9. Version Node incohérente : CI construit/teste avec Node 20.x, l'image de prod (`marketplace-ui/Dockerfile`) utilise Node 22.

### MOYENNE
10. Aucun healthcheck applicatif sur aucun service ; dépendances `depends_on` en `condition: service_started` (pas `service_healthy`).
11. Jenkins (8080) et SonarQube (9001) exposés sur `0.0.0.0` sans TLS ni restriction.
12. Durcissement Docker inégal : seul `product/Dockerfile` tourne en utilisateur non-root.
13. Pas de `.dockerignore` sur 5 des 7 services (`COPY . .` peut inclure `.git`, `target/`).

### FAIBLE
14. Tag `elasticsearch:9.5.2` à confirmer (numérotation atypique).
15. `product/Dockerfile` utilise `mvn` au lieu de `./mvnw`, seul cas.

### Cohérence des 2 derniers commits
- **elasticsearch** (`d79ec41`) : migration propre, pas de duplication, pas de régression technique — mais reste sans consommateur (cf. finding #6).
- **COMPOSE_PROJECT_NAME** (`584c87b`) : correction légitime, ne casse rien directement, mais révèle le drift de nommage déjà présent (`scripts/check.sh` désormais obsolète).

---

## Recommandations priorisées (tous axes confondus)

**Critique — à traiter avant tout déploiement**
1. Roter le mot de passe MongoDB et le secret JWT réels présents dans `.env.example`, purger l'historique git (BFG/`git filter-repo`).
2. Corriger `OrderService.java:122` (`&&` → `||`).
3. Committer `docker-compose.jenkins.yml` (manquant, CI cassée) ou retirer les références.
4. Renommer `Backend/orders/dockerfile` → `Dockerfile`.
5. Ajouter `orders` à la commande de déploiement Jenkins et aux conditions de test/SonarQube.
6. Corriger le doublon `routes[5]` dans la config gateway (route cart-service écrasée).
7. Signer/valider les en-têtes internes `X-User-Id`/`X-User-Role` (HMAC partagé ou revalidation JWT par service) au lieu de se reposer uniquement sur l'isolation réseau Docker.

**Important**
- Activer `xpack.security.enabled=true` sur Elasticsearch.
- Ajouter un rate limiting sur `/api/auth/**` (gateway).
- Décider si `search` doit être conteneurisé (Dockerfile + compose + Jenkins) ou retiré du scope actif.
- Compléter `.env.example` (variables manquantes) et retirer les variables obsolètes.
- Ajouter un handler `Exception.class` catch-all sur user/orders/media, créer un `GlobalExceptionHandler` pour search.
- Ajouter des timeouts explicites au `RestTemplate` d'orders ; ne plus avaler silencieusement les exceptions dans `CartService.getCart`.
- Vérifier si `spring-boot-starter-kafka` (search/pom.xml) compile réellement.
- Aligner les versions `spring-cloud` et retirer l'épinglage RC de `spring-boot-starter-validation`.

**Mineur**
- Adopter SLF4J partout (57 `System.out`/`System.err` à retirer), supprimer le code mort (`JwtAuthFilter.java`), corriger les typos, ajouter des `.dockerignore`, healthchecks, utilisateurs non-root sur les Dockerfiles restants.
- Ajouter des tests unitaires a minima sur orders (Cart/Order) et gateway (JwtFilter) — actuellement 0 test sur les 2 services les plus sensibles.

---

*Rapport généré par 3 agents en parallèle (code quality, sécurité, devops), lecture seule. Projet non enregistré dans le hub — pas de suivi historique automatique ; relancer l'audit manuellement lors du prochain point.*
