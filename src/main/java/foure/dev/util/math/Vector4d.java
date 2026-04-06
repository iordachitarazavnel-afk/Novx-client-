package foure.dev.util.math;

public record Vector4d(double x, double y, double z, double w) {
   public Vector4d(double x, double y, double z, double w) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.w = w;
   }

   public double x() {
      return this.x;
   }

   public double y() {
      return this.y;
   }

   public double z() {
      return this.z;
   }

   public double w() {
      return this.w;
   }
}
