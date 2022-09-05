package ru.javawebinar.topjava.util;

import ru.javawebinar.topjava.model.UserMeal;
import ru.javawebinar.topjava.model.UserMealWithExceed;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class UserMealsUtil {
    public static final List<UserMeal> USER_MEAL_LIST = Arrays.asList(
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

    public static List<UserMealWithExceed> getWithExceed(Collection<UserMeal> userMealList, int caloriesPerDay) {
        return MealsUtil.filteredByStreams(USER_MEAL_LIST, LocalTime.MIN, LocalTime.MAX, caloriesPerDay);
    }
    public static List<UserMealWithExceed> getFilteredWithExceed(Collection<UserMeal> userMealList, LocalTime startTime, LocalTime endTime, int caloriesPerDay) {
        return MealsUtil.filteredByStreams(USER_MEAL_LIST, startTime, endTime, caloriesPerDay);
    }
    public static UserMealWithExceed createWithExceed (UserMeal um, boolean exceed) {
        return new UserMealWithExceed(um.getDateTime(), um.getDescription(), um.getCalories(), exceed);
    }
}
