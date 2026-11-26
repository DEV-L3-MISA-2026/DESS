package com.dess.platform.web.view;

import com.dess.security.domain.RoleCatalog;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityViewTest
{
    @Test
    void countLabelUsesTheSingularForOneAccount()
    {
        SecurityView view = new SecurityView(
                List.of(new AccountRow("admin", "Administrator", "ADMIN", true)),
                Map.of(), Map.of());

        assertEquals("1 account.", view.getCountLabel());
    }

    @Test
    void theSubmittedRoleIsSelected()
    {
        SecurityView view = new SecurityView(List.of(),
                Map.of("role", RoleCatalog.PROFESSOR), Map.of());

        assertEquals(RoleCatalog.PROFESSOR, selectedRole(view));
    }

    @Test
    void noRoleIsSelectedOnAFirstVisit()
    {
        SecurityView view = new SecurityView(List.of(), Map.of(), Map.of());

        assertEquals("", selectedRole(view));
    }

    private static String selectedRole(SecurityView view)
    {
        for (Option option : view.getRoles())
        {
            if ("selected".equals(option.getSelectedAttribute()))
            {
                return option.getValue();
            }
        }

        return "";
    }
}
