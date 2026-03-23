package com.expensetracker.backend.service;

import com.expensetracker.backend.model.Category;
import com.expensetracker.backend.model.Expense;
import com.expensetracker.backend.model.User;
import com.expensetracker.backend.repository.ExpenseRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public List<Expense> getAllExpenses(Long userId) {
        return expenseRepository.findByUserId(userId);
    }

    public List<Expense> getExpensesByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return expenseRepository.findByUserIdAndExpenseDateBetween(userId, startDate, endDate);
    }

    @Transactional
    public Expense addExpense(Expense expense) {
        // Find existing category by name
        String categoryName = expense.getCategory() != null ? expense.getCategory().getName() : "Other";
        List<Category> categories = entityManager
            .createQuery("SELECT c FROM Category c WHERE c.name = :name AND c.user IS NULL", Category.class)
            .setParameter("name", categoryName)
            .getResultList();

        if (!categories.isEmpty()) {
            expense.setCategory(categories.get(0));
        } else {
            expense.setCategory(null);
        }

        // Set managed user
        if (expense.getUser() != null && expense.getUser().getId() != null) {
            User managedUser = entityManager.find(User.class, expense.getUser().getId());
            expense.setUser(managedUser);
        }

        return expenseRepository.save(expense);
    }

    @Transactional
    public Expense updateExpense(Long id, Expense updatedExpense) {
        Optional<Expense> existing = expenseRepository.findById(id);
        if (existing.isPresent()) {
            Expense expense = existing.get();
            expense.setAmount(updatedExpense.getAmount());
            expense.setDescription(updatedExpense.getDescription());
            expense.setExpenseDate(updatedExpense.getExpenseDate());

            // Find existing category
            String categoryName = updatedExpense.getCategory() != null ? updatedExpense.getCategory().getName() : "Other";
            List<Category> categories = entityManager
                .createQuery("SELECT c FROM Category c WHERE c.name = :name AND c.user IS NULL", Category.class)
                .setParameter("name", categoryName)
                .getResultList();

            if (!categories.isEmpty()) {
                expense.setCategory(categories.get(0));
            }

            return expenseRepository.save(expense);
        }
        throw new RuntimeException("Expense not found");
    }

    public void deleteExpense(Long id) {
        expenseRepository.deleteById(id);
    }
}