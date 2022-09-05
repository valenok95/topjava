package ru.javawebinar.topjava.util;

import ru.javawebinar.topjava.model.UserMeal;
import ru.javawebinar.topjava.model.UserMealWithExceed;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static ru.javawebinar.topjava.util.TimeUtil.isBetweenHalfOpen;

public class MealsUtil {
    public static void main(String[] args) throws InterruptedException {
        List<UserMeal> userMeals = Arrays.asList(
                new UserMeal(LocalDateTime.of(2020, Month.JANUARY, 30, 10, 0), "Завтрак", 500),
                new UserMeal(LocalDateTime.of(2020, Month.JANUARY, 30, 13, 0), "Обед", 1000),
                new UserMeal(LocalDateTime.of(2020, Month.JANUARY, 30, 20, 0), "Ужин", 500),
                new UserMeal(LocalDateTime.of(2020, Month.JANUARY, 31, 0, 0), "Еда на граничное значение", 100),
                new UserMeal(LocalDateTime.of(2020, Month.JANUARY, 31, 10, 0), "Завтрак", 1000),
                new UserMeal(LocalDateTime.of(2020, Month.JANUARY, 31, 13, 0), "Обед", 500),
                new UserMeal(LocalDateTime.of(2020, Month.JANUARY, 31, 20, 0), "Ужин", 410)
        );

        final LocalTime startTime = LocalTime.of(7, 0);
        final LocalTime endTime = LocalTime.of(12, 0);

        List<UserMealWithExceed> mealsTo = filteredByStreams(userMeals, startTime, endTime, 2000);
        mealsTo.forEach(System.out::println);

        System.out.println(filteredByCycles(userMeals, startTime, endTime, 2000));

        // Optional2 recursion
        System.out.println(filteredByRecursion(userMeals, startTime, endTime, 2000));
        System.out.println(filteredBySetterRecursion(userMeals, startTime, endTime, 2000));
        System.out.println(filteredByRecursionWithCycleAndRunnable(userMeals, startTime, endTime, 2000));

        //  Optional2 reference type
        //        System.out.println(filteredByAtomic(meals, startTime, endTime, 2000));  // or boolean[1]
        //        System.out.println(filteredByReflection(meals, startTime, endTime, 2000));

        //   Optional2 delayed execution
        //      System.out.println(filteredByClosure(meals, startTime, endTime, 2000));
        System.out.println(filteredByExecutor(userMeals, startTime, endTime, 2000));
        System.out.println(filteredByLock(userMeals, startTime, endTime, 2000));
        System.out.println(filteredByCountDownLatch(userMeals, startTime, endTime, 2000));
        System.out.println(filteredByPredicate(userMeals, startTime, endTime, 2000));
        System.out.println(filteredByConsumerChain(userMeals, startTime, endTime, 2000));

        //   Optional2 streams
        System.out.println(filteredByFlatMap(userMeals, startTime, endTime, 2000));
        System.out.println(filteredByCollector(userMeals, startTime, endTime, 2000));
    }

    public static List<UserMealWithExceed> filteredByStreams(List<UserMeal> userMeals, LocalTime startTime, LocalTime endTime, int caloriesPerDay) {
        Map<LocalDate, Integer> caloriesSumByDate = userMeals.stream()
                .collect(
                        Collectors.groupingBy(UserMeal::getDate, Collectors.summingInt(UserMeal::getCalories))
                        //                      Collectors.toMap(Meal::getDate, Meal::getCalories, Integer::sum)
                );

        return userMeals.stream()
                .filter(userMeal -> isBetweenHalfOpen(userMeal.getTime(), startTime, endTime))
                .map(userMeal -> createTo(userMeal, caloriesSumByDate.get(userMeal.getDate()) > caloriesPerDay))
                .collect(Collectors.toList());
    }

    public static List<UserMealWithExceed> filteredByCycles(List<UserMeal> userMeals, LocalTime startTime, LocalTime endTime, int caloriesPerDay) {

        final Map<LocalDate, Integer> caloriesSumByDate = new HashMap<>();
        userMeals.forEach(userMeal -> caloriesSumByDate.merge(userMeal.getDate(), userMeal.getCalories(), Integer::sum));

        final List<UserMealWithExceed> mealsTo = new ArrayList<>();
        userMeals.forEach(userMeal -> {
            if (isBetweenHalfOpen(userMeal.getTime(), startTime, endTime)) {
                mealsTo.add(createTo(userMeal, caloriesSumByDate.get(userMeal.getDate()) > caloriesPerDay));
            }
        });
        return mealsTo;
    }

    private static List<UserMealWithExceed> filteredByRecursion(List<UserMeal> userMeals, LocalTime startTime, LocalTime endTime, int caloriesPerDay) {
        ArrayList<UserMealWithExceed> result = new ArrayList<>();
        filterWithRecursion(new LinkedList<>(userMeals), startTime, endTime, caloriesPerDay, new HashMap<>(), result);
        return result;
    }

    private static void filterWithRecursion(LinkedList<UserMeal> userMeals, LocalTime startTime, LocalTime endTime, int caloriesPerDay,
                                            Map<LocalDate, Integer> dailyCaloriesMap, List<UserMealWithExceed> result) {
        if (userMeals.isEmpty()) return;

        UserMeal userMeal = userMeals.pop();
        dailyCaloriesMap.merge(userMeal.getDate(), userMeal.getCalories(), Integer::sum);
        filterWithRecursion(userMeals, startTime, endTime, caloriesPerDay, dailyCaloriesMap, result);
        if (isBetweenHalfOpen(userMeal.getTime(), startTime, endTime)) {
            result.add(createTo(userMeal, dailyCaloriesMap.get(userMeal.getDate()) > caloriesPerDay));
        }
    }

    private static List<UserMealWithExceed> filteredBySetterRecursion(List<UserMeal> userMeals, LocalTime startTime, LocalTime endTime, int caloriesPerDay) {
        class MealNode {
            private final MealNode prev;
            private final UserMealWithExceed mealTo;

            public MealNode(UserMealWithExceed mealTo, MealNode prev) {
                this.mealTo = mealTo;
                this.prev = prev;
            }

            public void setExcess() {
                mealTo.setExcess(true);
                if (prev != null) {
                    prev.setExcess();
                }
            }
        }

        Map<LocalDate, Integer> caloriesSumByDate = new HashMap<>();
        Map<LocalDate, MealNode> mealNodeByDate = new HashMap<>();
        List<UserMealWithExceed> mealsTo = new ArrayList<>();
        userMeals.forEach(userMeal -> {
            LocalDate localDate = userMeal.getDate();
            boolean excess = caloriesSumByDate.merge(localDate, userMeal.getCalories(), Integer::sum) > caloriesPerDay;
            if (TimeUtil.isBetweenHalfOpen(userMeal.getTime(), startTime, endTime)) {
                UserMealWithExceed userMealWithExceed = createTo(userMeal, excess);
                mealsTo.add(userMealWithExceed);
                if (!excess) {
                    MealNode prevNode = mealNodeByDate.get(localDate);
                    mealNodeByDate.put(localDate, new MealNode(userMealWithExceed, prevNode));
                }
            }
            if (excess) {
                MealNode mealNode = mealNodeByDate.remove(localDate);
                if (mealNode != null) {
                    // recursive set for all interval day meals
                    mealNode.setExcess();
                }
            }
        });
        return mealsTo;
    }

    public static List<UserMealWithExceed> filteredByRecursionWithCycleAndRunnable(List<UserMeal> userMeals, LocalTime startTime, LocalTime endTime, int caloriesPerDay) {
        Map<LocalDate, Integer> caloriesSumByDate = new HashMap<>();
        List<UserMealWithExceed> mealsTo = new ArrayList<>();
        Iterator<UserMeal> iterator = userMeals.iterator();

        new Runnable() {
            @Override
            public void run() {
                while (iterator.hasNext()) {
                    UserMeal userMeal = iterator.next();
                    caloriesSumByDate.merge(userMeal.getDate(), userMeal.getCalories(), Integer::sum);
                    if (isBetweenHalfOpen(userMeal.getTime(), startTime, endTime)) {
                        run();
                        mealsTo.add(createTo(userMeal, caloriesSumByDate.get(userMeal.getDate()) > caloriesPerDay));
                    }
                }
            }
        }.run();
        return mealsTo;
    }
    
    /*
        private static List<MealTo> filteredByAtomic(List<Meal> meals, LocalTime startTime, LocalTime endTime, int caloriesPerDay) {
            Map<LocalDate, Integer> caloriesSumByDate = new HashMap<>();
            Map<LocalDate, AtomicBoolean> exceededMap = new HashMap<>();

            List<MealTo> mealsTo = new ArrayList<>();
            meals.forEach(meal -> {
                AtomicBoolean wrapBoolean = exceededMap.computeIfAbsent(meal.getDate(), date -> new AtomicBoolean());
                Integer dailyCalories = caloriesSumByDate.merge(meal.getDate(), meal.getCalories(), Integer::sum);
                if (dailyCalories > caloriesPerDay) {
                    wrapBoolean.set(true);
                }
                if (isBetween(meal.getTime(), startTime, endTime)) {
                  mealsTo.add(createTo(meal, wrapBoolean));  // also change createTo and MealTo.excess
                }
            });
            return mealsTo;
        }

        private static List<MealTo> filteredByReflection(List<Meal> meals, LocalTime startTime, LocalTime endTime, int caloriesPerDay) throws NoSuchFieldException, IllegalAccessException {
            Map<LocalDate, Integer> caloriesSumByDate = new HashMap<>();
            Map<LocalDate, Boolean> exceededMap = new HashMap<>();
            Field field = Boolean.class.getDeclaredField("value");
            field.setAccessible(true);

            List<MealTo> mealsTo = new ArrayList<>();
            for (Meal meal : meals) {
                Boolean mutableBoolean = exceededMap.computeIfAbsent(meal.getDate(), date -> new Boolean(false));
                Integer dailyCalories = caloriesSumByDate.merge(meal.getDate(), meal.getCalories(), Integer::sum);
                if (dailyCalories > caloriesPerDay) {
                    field.setBoolean(mutableBoolean, true);
                }
                if (isBetweenHalfOpen(meal.getTime(), startTime, endTime)) {
                    mealsTo.add(createTo(meal, mutableBoolean));  // also change createTo and MealTo.excess
                }
            }
            return mealsTo;
        }

        private static List<MealTo> filteredByClosure(List<Meal> mealList, LocalTime startTime, LocalTime endTime, int caloriesPerDay) {
            final Map<LocalDate, Integer> caloriesSumByDate = new HashMap<>();
            List<MealTo> mealsTo = new ArrayList<>();
            mealList.forEach(meal -> {
                        caloriesSumByDate.merge(meal.getDate(), meal.getCalories(), Integer::sum);
                        if (isBetween(meal.getTime(), startTime, endTime)) {
                            mealsTo.add(createTo(meal, () -> (caloriesSumByDate.get(meal.getDate()) > caloriesPerDay))); // also change createTo and MealTo.excess
                        }
                    }
            );
            return mealsTo;
        }
    */

    private static List<UserMealWithExceed> filteredByExecutor(List<UserMeal> userMeals, LocalTime startTime, LocalTime endTime, int caloriesPerDay) throws InterruptedException {
        Map<LocalDate, Integer> caloriesSumByDate = new HashMap<>();
        List<Callable<Void>> tasks = new ArrayList<>();
        final List<UserMealWithExceed> mealsTo = Collections.synchronizedList(new ArrayList<>());

        userMeals.forEach(userMeal -> {
            caloriesSumByDate.merge(userMeal.getDate(), userMeal.getCalories(), Integer::sum);
            if (isBetweenHalfOpen(userMeal.getTime(), startTime, endTime)) {
                tasks.add(() -> {
                    mealsTo.add(createTo(userMeal, caloriesSumByDate.get(userMeal.getDate()) > caloriesPerDay));
                    return null;
                });
            }
        });
        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.invokeAll(tasks);
        executorService.shutdown();
        return mealsTo;
    }

    public static List<UserMealWithExceed> filteredByLock(List<UserMeal> userMeals, LocalTime startTime, LocalTime endTime, int caloriesPerDay) throws InterruptedException {
        Map<LocalDate, Integer> caloriesSumByDate = new HashMap<>();
        List<UserMealWithExceed> mealsTo = new ArrayList<>();
        ExecutorService executor = Executors.newCachedThreadPool();
        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        for (UserMeal userMeal : userMeals) {
            caloriesSumByDate.merge(userMeal.getDateTime().toLocalDate(), userMeal.getCalories(), Integer::sum);
            if (isBetweenHalfOpen(userMeal.getDateTime().toLocalTime(), startTime, endTime))
                executor.submit(() -> {
                    lock.lock();
                    mealsTo.add(createTo(userMeal, caloriesSumByDate.get(userMeal.getDateTime().toLocalDate()) > caloriesPerDay));
                    lock.unlock();
                });
        }
        lock.unlock();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        return mealsTo;
    }

    private static List<UserMealWithExceed> filteredByCountDownLatch(List<UserMeal> userMeals, LocalTime startTime, LocalTime endTime, int caloriesPerDay) throws InterruptedException {
        Map<LocalDate, Integer> caloriesSumByDate = new HashMap<>();
        List<UserMealWithExceed> mealsTo = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latchCycles = new CountDownLatch(userMeals.size());
        CountDownLatch latchTasks = new CountDownLatch(userMeals.size());
        for (UserMeal userMeal : userMeals) {
            caloriesSumByDate.merge(userMeal.getDateTime().toLocalDate(), userMeal.getCalories(), Integer::sum);
            if (isBetweenHalfOpen(userMeal.getDateTime().toLocalTime(), startTime, endTime)) {
                new Thread(() -> {
                    try {
                        latchCycles.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mealsTo.add(createTo(userMeal, caloriesSumByDate.get(userMeal.getDateTime().toLocalDate()) > caloriesPerDay));
                    latchTasks.countDown();
                }).start();
            } else {
                latchTasks.countDown();
            }
            latchCycles.countDown();
        }
        latchTasks.await();
        return mealsTo;
    }

    public static List<UserMealWithExceed> filteredByPredicate(List<UserMeal> userMeals, LocalTime startTime, LocalTime endTime, int caloriesPerDay) {
        Map<LocalDate, Integer> caloriesSumByDate = new HashMap<>();
        List<UserMealWithExceed> mealsTo = new ArrayList<>();

        Predicate<Boolean> predicate = b -> true;
        for (UserMeal userMeal : userMeals) {
            caloriesSumByDate.merge(userMeal.getDateTime().toLocalDate(), userMeal.getCalories(), Integer::sum);
            if (TimeUtil.isBetweenHalfOpen(userMeal.getDateTime().toLocalTime(), startTime, endTime)) {
                predicate = predicate.and(b -> mealsTo.add(createTo(userMeal, caloriesSumByDate.get(userMeal.getDateTime().toLocalDate()) > caloriesPerDay)));
            }
        }
        predicate.test(true);
        return mealsTo;
    }

    public static List<UserMealWithExceed> filteredByConsumerChain(List<UserMeal> userMeals, LocalTime startTime, LocalTime endTime, int caloriesPerDay) {
        Map<LocalDate, Integer> caloriesPerDays = new HashMap<>();
        List<UserMealWithExceed> result = new ArrayList<>();
        Consumer<Void> consumer = dummy -> {
        };

        for (UserMeal userMeal : userMeals) {
            caloriesPerDays.merge(userMeal.getDate(), userMeal.getCalories(), Integer::sum);
            if (TimeUtil.isBetweenHalfOpen(userMeal.getTime(), startTime, endTime)) {
                consumer = consumer.andThen(dummy -> result.add(createTo(userMeal, caloriesPerDays.get(userMeal.getDateTime().toLocalDate()) > caloriesPerDay)));
            }
        }
        consumer.accept(null);
        return result;
    }

    private static List<UserMealWithExceed> filteredByFlatMap(List<UserMeal> userMeals, LocalTime startTime, LocalTime endTime, int caloriesPerDay) {
        Collection<List<UserMeal>> list = userMeals.stream()
                .collect(Collectors.groupingBy(UserMeal::getDate)).values();

        return list.stream()
                .flatMap(dayMeals -> {
                    boolean excess = dayMeals.stream().mapToInt(UserMeal::getCalories).sum() > caloriesPerDay;
                    return dayMeals.stream().filter(userMeal ->
                                    isBetweenHalfOpen(userMeal.getTime(), startTime, endTime))
                            .map(userMeal -> createTo(userMeal, excess));
                }).collect(toList());
    }

    private static List<UserMealWithExceed> filteredByCollector(List<UserMeal> userMeals, LocalTime startTime, LocalTime endTime, int caloriesPerDay) {
        final class Aggregate {
            private final List<UserMeal> dailyMeals = new ArrayList<>();
            private int dailySumOfCalories;

            private void accumulate(UserMeal meal) {
                dailySumOfCalories += meal.getCalories();
                if (isBetweenHalfOpen(meal.getTime(), startTime, endTime)) {
                    dailyMeals.add(meal);
                }
            }

            // never invoked if the upstream is sequential
            private Aggregate combine(Aggregate that) {
                this.dailySumOfCalories += that.dailySumOfCalories;
                this.dailyMeals.addAll(that.dailyMeals);
                return this;
            }

            private Stream<UserMealWithExceed> finisher() {
                final boolean excess = dailySumOfCalories > caloriesPerDay;
                return dailyMeals.stream().map(userMeal -> createTo(userMeal, excess));
            }
        }

        Collection<Stream<UserMealWithExceed>> values = userMeals.stream()
                .collect(Collectors.groupingBy(UserMeal::getDate,
                        Collector.of(Aggregate::new, Aggregate::accumulate, Aggregate::combine, Aggregate::finisher))
                ).values();

        return values.stream().flatMap(identity()).collect(toList());
    }

    private static UserMealWithExceed createTo(UserMeal userMeal, boolean excess) {
        return new UserMealWithExceed(userMeal.getDateTime(), userMeal.getDescription(), userMeal.getCalories(), excess);
    }
}
