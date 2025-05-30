import java.util.Random;

public class Yut {
    public enum Result {빽도, 도, 개, 걸, 윷, 모}

    private static final Random random = new Random();
    private static final float PROB_FLAT = 0.6f;  // 평평한 면 확률

    /**
     * 네 개의 윷가락을 던져 실제 윷놀이 분포로 결과를 반환
     */
    public static Result throwRandom() {
        int sum = 0;
        // 평평한 면은 1, 둥근 면은 0으로 간주
        for (int i = 0; i < 4; i++) {
            if (random.nextFloat() <= PROB_FLAT) {
                sum += 1;  // 평평
            }
        }
        switch (sum) {
            case 0: return Result.모;       // 모두 둥근 면
            case 4: return Result.윷;      // 모두 평평 면
            case 3: return Result.걸;     // 3개 평평
            case 2: return Result.개;      // 2개 평평
            case 1:
                if(random.nextFloat() <= 0.25f){
                    return Result.빽도;  //4분의 1 확률로 빽도
                }
                return Result.도;
            default:
                // 예외 상황 방어
                return Result.빽도;
        }
    }

    /**
     * 특정 결과를 강제로 반환할 때 사용
     */
    public static Result throwSpecified(Result specified) {
        return specified;
    }
}