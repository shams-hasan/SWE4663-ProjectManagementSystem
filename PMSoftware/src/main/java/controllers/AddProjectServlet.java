package controllers;

import main.ProjectDatabase;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;

@WebServlet("/add-project")
public class AddProjectServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userID") == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        int ownerID = (int) session.getAttribute("userID");
        String projectName = request.getParameter("projectName");
        String description = request.getParameter("description");
        String status = request.getParameter("status");
        String riskLevel = request.getParameter("riskLevel");
        String dueDate = request.getParameter("dueDate");

        try {
            ProjectDatabase db = new ProjectDatabase();
            Connection conn = db.getConnection();

            // Insert into projects
            String sql = "INSERT INTO projects (ProjectName, Description, CreatedAt, OwnerID, Status, RiskLevel, DueDate, LoggedHours) " +
                    "VALUES (?, ?, NOW(), ?, ?, ?, ?, 0)";
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, projectName);
            stmt.setString(2, description);
            stmt.setInt(3, ownerID);
            stmt.setString(4, status);
            stmt.setString(5, riskLevel);
            stmt.setString(6, dueDate);
            stmt.executeUpdate();

            // Get the generated project ID
            ResultSet generatedKeys = stmt.getGeneratedKeys();
            int projectId = 0;
            if (generatedKeys.next()) {
                projectId = generatedKeys.getInt(1);
            }

            // Insert into team_members
            if (projectId > 0) {
                String teamSql = "INSERT INTO team_members (ProjectID, UserID, Role) VALUES (?, ?, ?)";
                try (PreparedStatement teamStmt = conn.prepareStatement(teamSql)) {
                    teamStmt.setInt(1, projectId);
                    teamStmt.setInt(2, ownerID);
                    teamStmt.setString(3, "owner");
                    teamStmt.executeUpdate();
                }
            }

            db.close();
            response.sendRedirect("pages/dashboard.jsp");

        } catch (Exception e) {
            throw new ServletException("Error adding project", e);
        }
    }
}
