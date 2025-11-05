read-write-routing-by-user (minimal demo)

Goal
- Show routing of reads/writes between a Postgres primary (leader) and replica (follower).
- Guarantee read-your-write for the writing user by routing their immediate reads to the leader.

Components
- Docker Compose: Bitnami Postgres primary + replica.
- Java (single file): simple router with sticky read-your-write for the writer; others read from follower.

Quick start
1) Start DBs:
   docker compose up -d
2) Compile:
   javac -cp . src/RouteByUser.java
3) Run (set env or edit defaults in code):
   LEADER_JDBC_URL=jdbc:postgresql://localhost:5432/appdb \
   FOLLOWER_JDBC_URL=jdbc:postgresql://localhost:5433/appdb \
   DB_USER=app \
   DB_PASSWORD=app_pw \
   java -cp src RouteByUser

Notes
- Writes go to leader. The writers subsequent reads stick to leader (read-your-write). Other users read from the follower, which may lag.
- Uses Bitnami images for straightforward replication configuration.

Visualize routing
- After running, you will see lines like:
  - "WRITE -> leader | user=userA | ..."
  - "READ  -> leader | requester=userA | target=userA | ..." (read-your-write)
  - "READ  -> follower | requester=userC | target=userA | ..." (may lag)

Environment
- Leader JDBC: jdbc:postgresql://localhost:5432/appdb
- Follower JDBC: jdbc:postgresql://localhost:5433/appdb
- User/Password: app / app_pw
