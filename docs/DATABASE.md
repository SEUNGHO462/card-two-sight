# MySQL·JDBC 경기 기록

## 현재 기능

- MySQL 8.4.13을 프로젝트의 `.tools/mysql/`에 설치했습니다. 시스템 서비스로 등록하지 않고 로컬 전용으로 실행합니다.
- MySQL Connector/J 9.5.0과 Java JDBC를 사용합니다. 접속은 기본 `127.0.0.1:3306`, DB는 `card_two_sight`입니다.
- 온라인 서버 또는 연습 모드가 FINAL을 확정하면 경기 ID·양쪽 닉네임·승수·목숨·최종 승자·라운드·종료 사유·경기 모드·AI 난이도·저장 일시를 `match_results`에 저장합니다.
- 경기 ID가 기본키이므로 같은 경기의 결과를 중복 저장하지 않습니다. SQL 값은 PreparedStatement로 전달합니다.
- 게임 화면의 기록 조회 버튼은 제거했습니다. `DB기록조회.bat` 또는 MySQL 터미널에서 저장된 기록을 조회합니다. 닉네임은 계정 식별자가 아닙니다.
- AI 연습 경기도 같은 테이블에 저장합니다. `match_mode`는 ONLINE / AI, `ai_difficulty`는 AI 경기의 EASY / NORMAL / HARD이며 온라인은 NULL입니다. 기존 설정과 누적 승패 JSON은 유지합니다.
- DB 작업은 별도 작업 스레드에서 처리합니다. 연결·저장 실패는 알리고 경기 진행은 유지합니다. 실패한 기록의 영구 재시도 큐는 아직 없습니다.

## 실행

1. `게임실행.bat` 또는 `서버실행.bat`은 이 PC에 설치한 로컬 MySQL을 먼저 확인하고 필요하면 숨김 실행합니다.
2. 온라인 또는 AI 연습 경기를 진행합니다. 온라인은 서버의 DB에, 연습은 이 PC의 DB에 저장합니다. 같은 DB를 사용하면 한 목록에서 함께 보입니다.
3. 종료 후 `DB기록조회.bat` 또는 MySQL 터미널에서 기록을 조회합니다. 게임 화면에 DB 조회 버튼은 표시하지 않습니다.

DB만 실행하려면 `DB실행.bat`, 종료하려면 `DB종료.bat`을 사용합니다. 게임 종료 후에도 DB는 유지되며 데이터는 디스크에 남습니다.

직접 Gradle로 서버를 실행할 때는 DB를 먼저 켭니다.

```powershell
.\DB실행.bat
.\gradlew.bat :server:run --args="54555"
```

`54555`는 게임 서버의 포트, `3306`은 MySQL의 포트입니다. 온라인 참가자는 게임 서버에만 접속하며 서버의 DB 계정을 전달받지 않습니다. AI 연습은 이 PC의 DB 설정을 사용합니다.

## 설정·데이터 위치

| 위치 | 내용 |
|---|---|
| `.runtime/db.properties` | JDBC URL·앱 계정·비밀번호 |
| `.runtime/mysql/my.ini` | MySQL 실행 설정 |
| `.runtime/mysql/data/` | 실제 DB 데이터 |
| `.runtime/mysql/admin.cnf` | 이 로컬 DB를 종료할 관리자 접속 정보 |
| `.tools/mysql/` | MySQL 실행 파일·라이선스 |

비밀번호를 코드에 넣지 않았으며 `.runtime/`와 `.tools/`는 Git 제외 대상입니다. 이 폴더를 삭제하면 DB 데이터나 실행 환경이 없어질 수 있습니다. DB 설정은 `C2S_DB_URL`, `C2S_DB_USER`, `C2S_DB_PASSWORD` 환경변수 또는 `-Dc2s.db.config=설정파일`로 대체할 수 있습니다.

이 설치는 해당 PC 전용입니다. 프로젝트를 다른 PC에 복사할 때는 그 PC의 MySQL과 접속 설정이 필요합니다. 로컬 설정의 TLS 비활성 옵션은 로컬 개발용이며 원격 DB에서는 서버 인증을 포함한 TLS 설정을 사용합니다.

## 확인한 범위

실제 MySQL과 TCP 클라이언트 두 개로 온라인 숫자 대결 후 이탈 종료까지 진행하여 저장·서버 조회·한글 보존·동일 경기 중복 방지·새 JDBC 연결에서 재조회까지 검사했습니다. 테스트 전용 `card_two_sight_test` DB를 사용하며 테스트 기록은 정리합니다.

```powershell
$env:C2S_DB_TEST='1'
.\gradlew.bat test
```

위 옵션이 없으면 MySQL 통합 테스트는 건너뜁니다. 일반 규칙·소켓 테스트는 DB 없이도 실행됩니다. 기존 테이블에는 match_mode와 ai_difficulty 컬럼을 추가했으며, 기존 기록은 ONLINE으로 유지합니다. 업데이트 전 종료한 AI 경기를 소급 복원하지 않습니다.

AI 추가 검증은 실제 연습 모드 한 판의 종료·저장·모드/난이도·기록 재조회와 중복 여부를 테스트 전용 DB로 확인합니다. DB 화면의 실제 클릭·렌더링과 두 물리 PC 접속은 별도 확인이 필요합니다.
