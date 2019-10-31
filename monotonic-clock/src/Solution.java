import org.jetbrains.annotations.NotNull;

public class Solution implements MonotonicClock {
    // First copy of my monotonic clock
    private final RegularInt a1 = new RegularInt(0);
    private final RegularInt a2 = new RegularInt(0);
    private final RegularInt a3 = new RegularInt(0);

    // Second copy of my monotonic clock
    private final RegularInt b1 = new RegularInt(0);
    private final RegularInt b2 = new RegularInt(0);
    private final RegularInt b3 = new RegularInt(0);

    @Override
    public void write(@NotNull Time time) {
        b1.setValue(time.getD1());
        b2.setValue(time.getD2());
        b3.setValue(time.getD3());

        a3.setValue(time.getD3());
        a2.setValue(time.getD2());
        a1.setValue(time.getD1());
    }

    @NotNull
    @Override
    public Time read() {
        final RegularInt newA1 = new RegularInt(a1.getValue());
        final RegularInt newA2 = new RegularInt(a2.getValue());
        final RegularInt newA3 = new RegularInt(a3.getValue());
        final Time a = new Time(newA1.getValue(), newA2.getValue(), newA3.getValue());
        final RegularInt newB3 = new RegularInt(b3.getValue());
        final RegularInt newB2 = new RegularInt(b2.getValue());
        final RegularInt newB1 = new RegularInt(b1.getValue());
        final Time b = new Time(newB1.getValue(), newB2.getValue(), newB3.getValue());

        if (a == b) {
            return a;
        } else {
            final RegularInt maxCrossValueIndex = new RegularInt(0);
            if (newB1.getValue() == newA1.getValue()) {
                maxCrossValueIndex.setValue(1);

                if (newB2.getValue() == newA2.getValue()) {
                    maxCrossValueIndex.setValue(2);

                    if (newB3.getValue() == newA3.getValue()) {
                        maxCrossValueIndex.setValue(3);
                    }
                }
            }

            switch (maxCrossValueIndex.getValue()) {
                case 0:
                    return new Time(newA1.getValue() + 1, 0, 0);
                case 1:
                    return new Time(newA1.getValue(), newA2.getValue() + 1, 0);
                case 2:
                    return new Time(newA1.getValue(), newA2.getValue(), newA3.getValue() + 1);
                default:
                    // case 3
                    return a;
            }
        }
    }
}