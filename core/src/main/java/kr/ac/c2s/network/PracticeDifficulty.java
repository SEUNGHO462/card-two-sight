package kr.ac.c2s.network;
public enum PracticeDifficulty {
    EASY("낮은 숫자 위주 · 특수카드 사용 안 함"),
    NORMAL("숫자와 특수카드를 무작위로 선택"),
    HARD("숫자 승률 고려 · 특수카드와 탄실 검사 활용");
    public final String description;
    PracticeDifficulty(String description){this.description=description;}
}
