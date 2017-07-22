// Just an AJAX update to find out if the file is finished yet.
import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/checkStatus")
public class checkStatus extends HttpServlet {
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        try {
            if (request.getParameter("getStatus") != null) {
                long lID = Long.parseLong(request.getParameter("getStatus"));
                Connection sqlConn = DriverManager.getConnection("jdbc:mysql://NOPE", "NOPE", "NOPE");
                PreparedStatement sqlStatement = sqlConn.prepareStatement("SELECT * FROM queue WHERE id=?");
                sqlStatement.setLong(1, lID);
                ResultSet rs = sqlStatement.executeQuery();
                String sProgress = null;
                String sStatus = null;
                String sOutname = null;
                if (rs.next()) {
                    sStatus = rs.getString("status");
                    sProgress = rs.getString("progress");
                    sOutname = rs.getString("outname");
                } else {
                    sStatus = "error";
                    sProgress = "null";
                    sOutname = "null";
                }
                sqlConn.close();
                String[] sArr = {sStatus, sProgress, sOutname};
                PrintWriter out = response.getWriter();
                out.print(new Gson().toJson(sArr));
                out.flush();
            }
        } catch (SQLException e) {
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }
}
