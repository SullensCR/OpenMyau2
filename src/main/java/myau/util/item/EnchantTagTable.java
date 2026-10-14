package myau.util.item;

import java.util.HashMap;

final class EnchantTagTable extends HashMap<Integer, EnchantTag> {
   EnchantTagTable() {
      this.put(0, new EnchantTag("Pr", 4));
      this.put(1, new EnchantTag("Fp", 4));
      this.put(2, new EnchantTag("Ff", 4));
      this.put(3, new EnchantTag("Bp", 4));
      this.put(4, new EnchantTag("Pp", 4));
      this.put(5, new EnchantTag("Re", 3));
      this.put(6, new EnchantTag("Aq", 1));
      this.put(7, new EnchantTag("Th", 3));
      this.put(8, new EnchantTag("Ds", 3));
      this.put(16, new EnchantTag("Sh", 5));
      this.put(17, new EnchantTag("Sm", 5));
      this.put(18, new EnchantTag("BoA", 5));
      this.put(19, new EnchantTag("Kb", 2));
      this.put(20, new EnchantTag("Fa", 2));
      this.put(21, new EnchantTag("Lo", 3));
      this.put(32, new EnchantTag("Ef", 5));
      this.put(33, new EnchantTag("St", 1));
      this.put(34, new EnchantTag("Ub", 3));
      this.put(35, new EnchantTag("Fo", 3));
      this.put(48, new EnchantTag("Po", 5));
      this.put(49, new EnchantTag("Pu", 2));
      this.put(50, new EnchantTag("Fl", 1));
      this.put(51, new EnchantTag("Inf", 1));
      this.put(61, new EnchantTag("LoS", 3));
      this.put(62, new EnchantTag("Lu", 3));
   }
}
