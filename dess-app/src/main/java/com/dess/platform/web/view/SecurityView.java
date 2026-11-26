package com.dess.platform.web.view;

import com.dess.security.domain.RoleCatalog;

import java.util.List;
import java.util.Map;

/** Account list and creation form on the security page. */
public final class SecurityView implements PageView
{
    private final PageChrome chrome;
    private final List<AccountRow> accounts;
    private final List<Option> roles;
    private final Map<String, String> form;
    private final Map<String, String> errors;
    private final String countLabel;
    private final EmptyState accountsEmpty;

    /**
     * @param accounts accounts
     * @param form submitted values
     * @param errors field errors
     */
    public SecurityView(List<AccountRow> accounts, Map<String, String> form,
                        Map<String, String> errors)
    {
        this.chrome = PageChrome.builder("Security", "Accounts and roles", "security",
                        "/WEB-INF/views/security/index.jsp")
                .description("Sign-in accounts and their roles. Accounts are separate from "
                        + "people records, and password values and hashes are never displayed.")
                .breadcrumb("Overview", "/app")
                .build();
        this.accounts = List.copyOf(accounts);
        this.form = Map.copyOf(form);
        this.errors = Map.copyOf(errors);
        this.roles = List.of(
                Option.of(RoleCatalog.ADMIN, "Administrator",
                        RoleCatalog.ADMIN.equals(form.get("role"))),
                Option.of(RoleCatalog.PROFESSOR, "Professor",
                        RoleCatalog.PROFESSOR.equals(form.get("role"))));
        this.countLabel = Views.count(accounts.size(), "account", "accounts") + ".";
        this.accountsEmpty = EmptyState.of("No account exists.",
                "Set DESS_ADMIN_PASSWORD before starting the application to seed the "
                        + "administrator account, or create one below.");
    }

    @Override
    public PageChrome getChrome()
    {
        return chrome;
    }

    public List<AccountRow> getAccounts()
    {
        return accounts;
    }

    public List<Option> getRoles()
    {
        return roles;
    }

    public Map<String, String> getForm()
    {
        return form;
    }

    public Map<String, String> getErrors()
    {
        return errors;
    }

    public String getCountLabel()
    {
        return countLabel;
    }

    public EmptyState getAccountsEmpty()
    {
        return accountsEmpty;
    }
}
