package ru.javawebinar.topjava.web;

import ru.javawebinar.topjava.model.UserMealWithExceed;
import ru.javawebinar.topjava.util.MealsUtil;
import ru.javawebinar.topjava.util.UserMealsUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class MealServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log("getAll");
        request.setAttribute("mealList", UserMealWithExceed(UserMealsUtil.USER_MEAL_LIST));
        request.getRequestDispatcher("meals.jsp").forward(request, response);

    }
}
