package Felix;

import java.util.Arrays;

class Util {
  static int random(int max) {
    return (int)(Math.floor(Math.random() * (double)(max)));
  }

  static <T> T[] randomShuffle(T[] arr) {
    T[] xs = arr.clone();
    for (int i = xs.length - 1; i > 0; --i) {
      int j = random(i + 1);
      T temp = xs[i];
      xs[i] = xs[j];
      xs[j] = temp;
    }
    return xs;
  }

  static <T> T[] removeElement(T[] arr, T elem) {
    T[] xs = arr.clone();
    int end = 0;
    for (int i = 0; i < xs.length; ++i) {
      if (!elem.equals(xs[i])) {
        xs[end] = xs[i];
        ++end;
      }
    }
    return Arrays.copyOfRange(xs, 0, end);
  }
}
