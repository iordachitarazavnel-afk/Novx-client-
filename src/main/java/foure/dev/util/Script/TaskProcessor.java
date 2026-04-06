package foure.dev.util.Script;

import foure.dev.module.api.Function;

import java.util.Comparator;
import java.util.Objects;
import java.util.PriorityQueue;
import lombok.Generated;

public class TaskProcessor<T> {
   public int tickCounter = 0;
   public PriorityQueue<TaskProcessor.Task<T>> activeTasks =
           new PriorityQueue<>(Comparator.comparingInt((TaskProcessor.Task<T> t) -> t.priority).reversed());

   public void tick(int deltaTime) {
      this.tickCounter += deltaTime;
   }

   public void addTask(TaskProcessor.Task<T> task) {
      this.activeTasks.removeIf((r) -> {
         return Objects.equals(r.provider, task.provider);
      });
      task.expiresIn += this.tickCounter;
      this.activeTasks.add(task);
   }

   public T fetchActiveTaskValue() {
      while (true) {
         while (!this.activeTasks.isEmpty()) {
            TaskProcessor.Task<T> task = this.activeTasks.peek();

            if (task == null) {
               this.activeTasks.poll();
               continue;
            }

            boolean isExpired = task.expiresIn <= this.tickCounter;
            boolean isProviderDisabled = task.provider != null && !task.provider.isToggled();

            if (!isExpired && !isProviderDisabled) {
               break;
            }

            this.activeTasks.poll();
         }

         if (this.activeTasks.isEmpty()) {
            return null;
         }

         return this.activeTasks.peek().value;
      }
   }

   public static class Task<T> {
      private int expiresIn;
      private final int priority;
      private final Function provider;
      private final T value;

      @Generated
      public String toString() {
         int var10000 = this.expiresIn;
         return "TaskProcessor.Task(expiresIn=" + var10000 + ", priority=" + this.priority + ", provider=" + String.valueOf(this.provider) + ", value=" + String.valueOf(this.value) + ")";
      }

      @Generated
      public Task(int expiresIn, int priority, Function provider, T value) {
         this.expiresIn = expiresIn;
         this.priority = priority;
         this.provider = provider;
         this.value = value;
      }
   }
}
