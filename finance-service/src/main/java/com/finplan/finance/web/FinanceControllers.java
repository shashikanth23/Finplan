package com.finplan.finance.web;

import com.finplan.finance.service.*;
import com.finplan.finance.web.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** All endpoints act on the authenticated user's own data; the user id never comes from the request. */
public final class FinanceControllers {
    private FinanceControllers() {}

    @RestController
    @RequestMapping("/api/profile")
    public static class ProfileController {
        private final ProfileService service;
        public ProfileController(ProfileService service) { this.service = service; }

        @GetMapping public ProfileResponse get(Authentication auth) { return service.view(CurrentUser.id(auth)); }

        @PutMapping public ProfileResponse put(Authentication auth, @Valid @RequestBody ProfileRequest r) {
            return service.save(CurrentUser.id(auth), r);
        }
    }

    @RestController
    @RequestMapping("/api/incomes")
    public static class IncomeController {
        private final IncomeService service;
        public IncomeController(IncomeService service) { this.service = service; }

        @GetMapping public List<IncomeResponse> list(Authentication auth) { return service.list(CurrentUser.id(auth)); }

        @PostMapping @ResponseStatus(HttpStatus.CREATED)
        public IncomeResponse create(Authentication auth, @Valid @RequestBody IncomeRequest r) {
            return service.create(CurrentUser.id(auth), r);
        }

        @PutMapping("/{id}")
        public IncomeResponse update(Authentication auth, @PathVariable UUID id, @Valid @RequestBody IncomeRequest r) {
            return service.update(CurrentUser.id(auth), id, r);
        }

        @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
        public void delete(Authentication auth, @PathVariable UUID id) { service.delete(CurrentUser.id(auth), id); }
    }

    @RestController
    @RequestMapping("/api/expenses")
    public static class ExpenseController {
        private final ExpenseService service;
        public ExpenseController(ExpenseService service) { this.service = service; }

        @GetMapping public List<ExpenseResponse> list(Authentication auth) { return service.list(CurrentUser.id(auth)); }

        @PostMapping @ResponseStatus(HttpStatus.CREATED)
        public ExpenseResponse create(Authentication auth, @Valid @RequestBody ExpenseRequest r) {
            return service.create(CurrentUser.id(auth), r);
        }

        @PutMapping("/{id}")
        public ExpenseResponse update(Authentication auth, @PathVariable UUID id, @Valid @RequestBody ExpenseRequest r) {
            return service.update(CurrentUser.id(auth), id, r);
        }

        @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
        public void delete(Authentication auth, @PathVariable UUID id) { service.delete(CurrentUser.id(auth), id); }
    }

    @RestController
    @RequestMapping("/api/debts")
    public static class DebtController {
        private final DebtService service;
        public DebtController(DebtService service) { this.service = service; }

        @GetMapping public List<DebtResponse> list(Authentication auth) { return service.list(CurrentUser.id(auth)); }

        @PostMapping @ResponseStatus(HttpStatus.CREATED)
        public DebtResponse create(Authentication auth, @Valid @RequestBody DebtRequest r) {
            return service.create(CurrentUser.id(auth), r);
        }

        @PutMapping("/{id}")
        public DebtResponse update(Authentication auth, @PathVariable UUID id, @Valid @RequestBody DebtRequest r) {
            return service.update(CurrentUser.id(auth), id, r);
        }

        @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
        public void delete(Authentication auth, @PathVariable UUID id) { service.delete(CurrentUser.id(auth), id); }
    }

    @RestController
    @RequestMapping("/api/goals")
    public static class GoalController {
        private final GoalService service;
        public GoalController(GoalService service) { this.service = service; }

        @GetMapping public List<GoalResponse> list(Authentication auth) { return service.list(CurrentUser.id(auth)); }

        @PostMapping @ResponseStatus(HttpStatus.CREATED)
        public GoalResponse create(Authentication auth, @Valid @RequestBody GoalRequest r) {
            return service.create(CurrentUser.id(auth), r);
        }

        @PutMapping("/{id}")
        public GoalResponse update(Authentication auth, @PathVariable UUID id, @Valid @RequestBody GoalRequest r) {
            return service.update(CurrentUser.id(auth), id, r);
        }

        @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
        public void delete(Authentication auth, @PathVariable UUID id) { service.delete(CurrentUser.id(auth), id); }
    }

    @RestController
    @RequestMapping("/api/dashboard")
    public static class DashboardController {
        private final DashboardService service;
        public DashboardController(DashboardService service) { this.service = service; }

        @GetMapping
        public DashboardService.DashboardResponse get(Authentication auth,
                                                      @RequestHeader("Authorization") String bearer) {
            return service.get(CurrentUser.id(auth), bearer);
        }
    }
}
