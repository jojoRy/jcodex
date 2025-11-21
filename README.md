# JCodex

JCodex는 마인크래프트 서버용 도감/수집 플러그인으로, 플레이어가 아이템을 수집하며 보상을 받고, MMOItems 스탯 보상을 지속적으로 유지하도록 설계되었습니다. Vault 결제 연동을 필수로 사용하며, MMOItems/ItemsAdder와 연동해 스탯과 커스텀 아이템을 활용할 수 있습니다.

## 주요 특징
- `/도감` GUI를 통한 플레이어 도감 확인 및 진행도 관리【F:src/main/java/kr/jjory/jcodex/command/CodexCommand.java†L12-L34】
- `/도감설정` GUI를 통한 관리자용 보상/마일스톤 설정【F:src/main/java/kr/jjory/jcodex/command/CodexAdminCommand.java†L12-L34】
- MAIN 서버에서 `/도감리로드`로 YAML 설정과 데이터베이스를 동기화하고 재적용【F:src/main/java/kr/jjory/jcodex/command/CodexReloadCommand.java†L12-L48】
- MAIN 서버에서 `/도감내보내기`로 DB 도감 데이터를 YAML로 내보내기【F:src/main/java/kr/jjory/jcodex/command/CodexExportCommand.java†L12-L43】
- Vault 필수, MMOItems/ItemsAdder 소프트 의존성 및 MySQL·Redis 기반 다중 서버 동기화 지원【F:src/main/resources/plugin.yml†L1-L23】【F:src/main/resources/config.yml†L1-L39】【F:src/main/java/kr/jjory/jcodex/JCodexPlugin.java†L37-L61】

## 요구 사항
- 서버 버전: Paper 1.21.x 호환 (api-version 1.21)【F:src/main/resources/plugin.yml†L1-L8】
- 필수 플러그인: Vault【F:src/main/resources/plugin.yml†L1-L8】
- 선택 플러그인: MMOItems(스탯 보상), ItemsAdder(커스텀 아이템)【F:src/main/resources/plugin.yml†L1-L8】
- 데이터베이스: MySQL (HikariCP 커넥션 풀 사용)【F:src/main/java/kr/jjory/jcodex/config/MainConfig.java†L13-L44】
- 선택: Redis(다중 서버 실시간 동기화)【F:src/main/resources/config.yml†L15-L27】【F:src/main/java/kr/jjory/jcodex/config/MainConfig.java†L20-L43】

## 설치 및 빌드
1. 저장소를 클론한 후 의존성을 받습니다.
2. 쉘에서 다음 명령으로 섀도우 JAR을 빌드합니다.
   ```bash
   ./gradlew shadowJar
   ```
   결과물은 `build/libs/jcodex-1.0.0.jar` 형태로 생성됩니다.【F:build.gradle.kts†L1-L44】
3. 생성된 JAR을 서버 `plugins/` 폴더에 배치하고 서버를 재시작합니다.

## 설정
플러그인은 시작 시 `config.yml`을 생성합니다. 주요 항목은 아래와 같습니다.【F:src/main/resources/config.yml†L1-L39】
- `general.language`: 메시지 언어 (기본 `ko_KR`).
- `general.debug`: 디버그 로그 출력 여부.
- `mysql`: MySQL 연결 정보(호스트, 포트, 데이터베이스, 계정, 풀 크기).【F:src/main/java/kr/jjory/jcodex/config/MainConfig.java†L13-L44】
- `redis`: Redis 실시간 동기화 설정(활성화 여부, 호스트, 포트, 비밀번호, 채널).【F:src/main/resources/config.yml†L15-L27】
- `multi-server.mode`: `MAIN` 또는 `SUB` 로 지정. MAIN은 YAML ↔ DB 동기화와 명령 실행을 담당하고 SUB는 Redis 수신 또는 폴백 주기 동기화를 수행합니다.【F:src/main/java/kr/jjory/jcodex/JCodexPlugin.java†L59-L80】【F:src/main/resources/config.yml†L15-L27】
- `register.consume-item`: 도감 등록 시 아이템 소모 여부.
- `stats.order` 및 `stats.defs`: GUI 표시 순서와 스탯 표시 이름/소수점 자리수를 정의합니다.【F:src/main/resources/config.yml†L29-L52】【F:src/main/java/kr/jjory/jcodex/config/MainConfig.java†L25-L66】

## 사용법
### 플레이어
- `/도감` 명령어로 도감 GUI를 열어 자신의 수집 현황을 확인합니다.【F:src/main/java/kr/jjory/jcodex/command/CodexCommand.java†L12-L34】

### 관리자
- `/도감설정`: 보상 및 마일스톤 설정 GUI를 엽니다.【F:src/main/java/kr/jjory/jcodex/command/CodexAdminCommand.java†L12-L34】
- `/도감리로드`: MAIN 서버에서 YAML 설정을 다시 읽고 DB와 동기화한 뒤 Redis를 통해 리로드를 전파합니다.【F:src/main/java/kr/jjory/jcodex/command/CodexReloadCommand.java†L24-L48】【F:src/main/java/kr/jjory/jcodex/JCodexPlugin.java†L59-L77】
- `/도감내보내기`: MAIN 서버에서 DB의 도감/마일스톤 데이터를 YAML로 내보냅니다.【F:src/main/java/kr/jjory/jcodex/command/CodexExportCommand.java†L24-L43】

### 권한
- `jcodex.user`: `/도감` 사용 권한.【F:src/main/resources/plugin.yml†L9-L23】
- `jcodex.admin`: 관리자 명령어 및 GUI 권한.【F:src/main/resources/plugin.yml†L9-L23】

## 다중 서버 운용
- MAIN 서버는 부팅 시 YAML 데이터를 DB와 자동 동기화하고, 리로드/내보내기 명령을 실행할 수 있습니다.【F:src/main/java/kr/jjory/jcodex/JCodexPlugin.java†L59-L80】【F:src/main/java/kr/jjory/jcodex/command/CodexReloadCommand.java†L24-L48】
- SUB 서버는 Redis 메시지 또는 폴백 주기로 최신 데이터를 가져와 플레이어에게 동일한 도감/보상을 제공합니다.【F:src/main/resources/config.yml†L15-L27】【F:src/main/java/kr/jjory/jcodex/config/MainConfig.java†L20-L43】

## MMOItems 스탯 보상
플레이어가 접속하면 부여된 MMOItems 스탯이 누락되지 않도록 확인 후 재적용하며, 이미 적용된 스탯은 중복 적용되지 않도록 관리합니다.【F:src/main/java/kr/jjory/jcodex/JCodexPlugin.java†L53-L76】【F:src/main/java/kr/jjory/jcodex/service/PlayerStatService.java†L24-L96】
관리자가 도감 항목을 삭제하면 해당 항목을 등록했던 모든 플레이어의 누적 스탯을 차감하고, 온라인 플레이어에게 즉시 재적용하여 삭제된 보상이 남지 않습니다.【F:src/main/java/kr/jjory/jcodex/gui/CodexAdminGUI.java†L137-L180】【F:src/main/java/kr/jjory/jcodex/service/PlayerStatService.java†L62-L96】

