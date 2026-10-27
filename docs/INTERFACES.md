# 연결 규격 v3 — 시작 카드 선택 및 직접 장전

Protocol.java가 공통 기준입니다. TCP UTF-8 한 줄 JSON, 메시지 최대 16,384자입니다. 플레이어 번호는 접속 세션으로 정하며 클라이언트가 남의 번호를 지정할 수 없습니다.

## 입력 Command

| action | 필드 | 호출 단계 |
|---|---|---|
| CREATE / JOIN | name, characterId, password | 방 외부 |
| READY | 없음 | 대기방, 준비 토글 |
| START | 없음 | 두 사람 준비 완료 후 방장만 |
| LOADOUT | loadout: 서로 다른 Card 2개 배열 | LOADOUT_SELECT, 1회 확정 |
| TILE | number 1~9 | TILE_SELECT |
| SPECIAL | card 또는 null, 예측이면 number 1~9 | SPECIAL_SELECT |
| NEXT | 없음 | ROUND_RESULT / ROULETTE_RESULT |
| LOAD_AMMO | number 0~8, 비어 있는 본인 탄실 | AMMO_LOAD, 추가 실탄이 있을 때만 |
| HINT | hint, number 0~8 | ROULETTE_SELECT |
| CHAMBER | number 0~8 | ROULETTE_SELECT, 회전 후 격발 버튼을 누를 때 |
| REMATCH | 없음 | FINAL, 양쪽 요청 필요 |
| LEAVE | 없음 | 연결 종료 |

card는 REVERSE / SCOUT / DISGUISE / PREDICT, hint는 RANGE(4칩) / INSPECT(8칩)입니다. 제거된 COMPARE / ADJACENT 요청은 거부하고 칩을 차감하지 않습니다.
화면의 탄실 번호는 1~9이고 통신 값은 0~8입니다.

## 출력 Event

- STATE: state에 개인별 Snapshot.
- ERROR: message에 오류 설명. 현재 연결을 유지합니다.
- CLOSED: message에 종료 이유. 접속 화면으로 이동합니다.

## 개인별 Snapshot

- you: 자신의 번호 0/1, players: 이름·캐릭터·생명·승수·남은 장수·선택/확정 상태.
- players[].loadoutReady: 시작 카드 선택 확정 여부. 상대는 확정 여부와 장수만 확인합니다.
- hand / cards / ownTile / ownChamber: 본인 정보만.
- phase / round / roundWinner / winner / draws / reason: 진행과 판정.
- opponentParity: 판정 후 공개할 홀짝. 위장 효과가 반영된 값.
- history / privateInfo / boughtHints / knownChambers: 해당 사용자용 정보.
- pendingAmmo: 본인이 추가 장전할 발 수. ammoChoice: 본인이 선택해 장전한 번호(0~8), 없으면 -1.
- loadedChambers: AMMO_LOAD 단계에서 본인의 실제 기존 실탄 위치만. 회전 후 실제 배치는 전송하지 않습니다.
- hintHistory: 본인 구매 내역을 R번호와 함께 경기 동안 유지. 다른 사용자에게 전송하지 않습니다.
- hit: 룰렛 결과의 두 사람 피격 여부. 준비 중에는 실제 배치 정보가 아닙니다.
- winner와 roundWinner: 0/1 승자, -1 무승부, -2 미판정.

상대 숫자·상대 특수카드 종류·전체 실탄 배치는 보내지 않습니다. 로컬 연습도 같은 Snapshot을 사용합니다.

## 화면 연결

C2SGame.accept(Snapshot)가 phase를 보고 LobbyScreen / CardSelectScreen / GameScreen / RouletteScreen / FinalResultScreen으로 전환합니다.
각 화면의 accept는 서버 응답을 기준으로 다시 그립니다. 버튼에 이름을 부여해 SmokeTour에서 실제 입력 경로를 검증합니다.
