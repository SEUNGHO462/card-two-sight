# 연결 규격 — 베팅·위장 선택·공용 칩

Protocol.java가 공통 기준입니다. TCP UTF-8 한 줄 JSON, 메시지 최대 16,384자입니다. 플레이어 번호는 접속 세션으로 정하며 클라이언트가 남의 번호를 지정할 수 없습니다.

## 입력 Command

| action | 필드 | 호출 단계 |
|---|---|---|
| CREATE / JOIN | name, characterId, password | 방 외부 |
| READY | 없음 | 대기방, 준비 토글 |
| START | 없음 | 두 사람 준비 완료 후 방장만 |
| LOADOUT | loadout: 서로 다른 Card 2개 배열 | LOADOUT_SELECT, 1회 확정 |
| BET | number 1~3, 잔액 0이면 0 | BET_SELECT, 라운드당 한 번 확정 |
| TILE | number 1~9 | TILE_SELECT |
| SPECIAL | card 또는 null, 예측이면 number 1~9, 위장이면 disguiseOdd(boolean) | SPECIAL_SELECT |
| NEXT | 없음 | ROUND_RESULT / ROULETTE_RESULT |
| LOAD_AMMO | number 0~8, 비어 있는 본인 탄실 | AMMO_LOAD, 추가 실탄이 있을 때만 |
| HINT | hint, number 0~8 | ROULETTE_SELECT |
| CHAMBER | number 0~8 | ROULETTE_SELECT, 회전 후 격발 버튼을 누를 때 |
| REMATCH | 없음 | FINAL, 양쪽 요청 필요 |
| HISTORY | 없음 | 온라인 방 입장 후 또는 연습 중 DB 기록 조회 |
| LEAVE | 없음 | 연결 종료 |

card는 REVERSE / SCOUT / DISGUISE / PREDICT, hint는 RANGE(4칩) / INSPECT(8칩)입니다. 제거된 COMPARE / ADJACENT 요청은 거부하고 칩을 차감하지 않습니다.
화면의 탄실 번호는 1~9이고 통신 값은 0~8입니다. 힌트는 라운드당 두 종류 중 하나만 구매할 수 있습니다.

`Command.disguise(true)`는 홀수, `false`는 짝수 표시입니다. `disguiseOdd`가 없는 구형 명령은 기존 반전 방식으로 처리하지만, 현재 UI와 AI는 표시할 홀짝을 명시합니다. 실제 숫자와 판정에는 영향을 주지 않습니다.

## 출력 Event

- STATE: state에 개인별 Snapshot.
- ERROR: message에 오류 설명. 현재 연결을 유지합니다.
- HISTORY: records에 현재 연결된 DB의 최근 경기 최대 20개. MatchRecord의 matchId·playerA/B·winsA/B·winner·finishedAt(UTC ISO 시각)·reason·matchMode(ONLINE/AI)·aiDifficulty(AI만 EASY/NORMAL/HARD)를 전달합니다. DB 계정은 전달하지 않습니다.
- CLOSED: message에 종료 이유. 접속 화면으로 이동합니다.

## 개인별 Snapshot

- you: 자신의 번호 0/1, players: 이름·캐릭터·생명·승수·남은 장수·선택/확정 상태.
- bettingEnabled: 베팅 사용 여부. 게임에서 사용하는 5인자 GameEngine 생성은 true이며, 베팅 없는 기존 규칙 검증용 생성자도 남아 있습니다.
- players[].chips: 베팅·힌트 공용 보유 칩. 실제 경기 시작은 30개입니다. 별도 bettingChips 필드는 없습니다.
- players[].betReady / bet: 베팅 확정 여부와 금액. 상대 금액은 둘 다 확정한 뒤 공개합니다.
- ownBet / opponentBet / matchedBet: 본인 베팅, 공개된 상대 베팅, 두 금액 중 최솟값.
- chipHistory: 본인의 베팅 차감·반환·획득·힌트 차감 내역. history의 경기 기록과 별개입니다.
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

상대의 정확한 숫자·미사용 특수카드 종류·전체 실탄 배치는 보내지 않습니다. 양쪽 사용 특수카드 이름은 라운드 판정 후 history에 포함합니다. 특수카드 선택 중에는 공개하지 않습니다. 로컬 연습도 같은 Snapshot을 사용합니다.

## 진행 단계와 베팅 정산

LOADOUT_SELECT → BET_SELECT → TILE_SELECT → SPECIAL_SELECT → ROUND_RESULT → AMMO_LOAD(필요할 때) → ROULETTE_SELECT → ROULETTE_RESULT → 다음 BET_SELECT 또는 FINAL.

베팅 확정 시 즉시 차감합니다. 판정 후 초과분을 반환하고 승자에게 최솟값의 두 배를 지급하며, 무승부는 전액 반환합니다. 대기 중 상대 잔액은 상대 베팅 차감 전으로 보정하여 비공개 베팅액이 잔액으로 드러나지 않도록 합니다. 상대 금액을 공개한 후에는 실제 잔액을 표시합니다. 자세한 예시와 이탈 시 처리는 RULES.md를 따릅니다.

## 화면 연결

C2SGame.accept(Snapshot)가 phase를 보고 LobbyScreen / CardSelectScreen / GameScreen / RouletteScreen / FinalResultScreen으로 전환합니다.
각 화면의 accept는 서버 응답을 기준으로 다시 그립니다. 버튼에 이름을 부여해 SmokeTour에서 실제 입력 경로를 검증합니다.


## 연습 AI의 라운드 관찰 정보

`Snapshot.opponentObservations`는 본인의 완료 라운드 관찰 기록입니다. 각 `RoundObservation`은 `round`, `possibleOpponentTiles`, `knownRemainingTile`(예측으로 공개된 남은 타일, 없으면 0)을 포함합니다. 상대 숫자 후보는 공개 홀짝·승패·사용 특수카드와 본인이 얻은 정찰·예측 정보로만 구성합니다. 위장 사용 라운드는 참·거짓 홀짝 모두 가능하므로 표시 홀짝으로 후보를 좁히지 않습니다. 상대의 숨겨진 선택이나 정확한 전체 패는 보내지 않습니다. 예측 정보의 미사용 조건은 해당 기록 라운드 시점에만 적용합니다.

라운드가 바뀌어도 누적하고 재경기에서 초기화합니다. 새 필드는 추가 형식이며 화면의 기존 표시 문자열에 의존하지 않습니다. 옛 기록에 필드가 없으면 AI는 균등 가정으로 평가합니다. 자세한 판단 규칙은 `AI_STRATEGY.md`를 참고하세요.
