package com.DM.dairyManagement.controller;

import java.io.File;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.DM.dairyManagement.model.User;
import com.DM.dairyManagement.service.BillService;
import com.DM.dairyManagement.service.CompanyService;
import com.DM.dairyManagement.service.PaymentService;
import com.DM.dairyManagement.service.ProductService;
import com.DM.dairyManagement.service.RetailerService;
import com.DM.dairyManagement.service.UnitService;
import com.DM.dairyManagement.service.UserService;
import com.DM.dairyManagement.service.WholesalerService;

import jakarta.servlet.http.HttpSession;

@Controller
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private CompanyService companyService;
    @Autowired
    private ProductService productService;
    @Autowired
    private BillService billService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private RetailerService retailerService;
    @Autowired
    private WholesalerService wholesalerService;
    @Autowired
    private UnitService unitService;
    

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @GetMapping("/signup")
    public String showRegisterPage() {
        return "signup";
    }

    @PostMapping("/signup")
    public String registerUser(User user, @RequestParam("photo") MultipartFile photo) throws IOException {
        if (!photo.isEmpty()) {
            String originalFilename = photo.getOriginalFilename();
            if (originalFilename != null) {
                String fileName = StringUtils.cleanPath(originalFilename);
                String uploadDir = System.getProperty("user.dir") + "/uploads/";
                File uploadDirFile = new File(uploadDir);
                if (!uploadDirFile.exists()) {
                    uploadDirFile.mkdirs();
                }
                File file = new File(uploadDir + fileName);
                photo.transferTo(file);
                user.setPhotoPath("/uploads/" + fileName);
            }
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userService.saveUser(user);
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }
    
    /**
     * Fallback for index or home page
     * This is an additional handler in case the HomeController doesn't catch it
     */
    @GetMapping({"/index", "/"})
    public String redirectToLogin() {
        return "redirect:/login";
    }

    // Admin credentials from environment variables or defaults
    private String getAdminEmail() {
        return System.getProperty("ADMIN_EMAIL", "admin@gmail.com");
    }
    
    private String getAdminPassword() {
        return System.getProperty("ADMIN_PASSWORD", "pass");
    }

    @PostMapping("/login")
    public String loginUser(@RequestParam("email") String email,
                            @RequestParam("password") String password,
                            Model model, HttpSession session) {

        // Trim inputs to remove any whitespace
        email = email.trim();
        password = password.trim();
        
        System.out.println("Login attempt with email: " + email);

        // Check for admin login
        if (email.equals(getAdminEmail())) {
            User user = userService.getUserByEmail(email);
            
            // Create admin if not exists
            if (user == null) {
                user = new User();
                user.setName("Admin");
                user.setEmail(getAdminEmail());
                user.setMobile("0000000000");
                // Don't encode password here as saveUser will encode it
                user.setPassword(getAdminPassword());
                user.setPhotoPath("/images/userLogo.jpg");
                userService.saveUser(user);
                System.out.println("Created admin account");
            }
            
            // For admin, check if password matches the encoded password in database
            // Only allow default password if the stored password is still the encoded default
            boolean isDefaultPasswordStored = passwordEncoder.matches(getAdminPassword(), user.getPassword());
            
            if (passwordEncoder.matches(password, user.getPassword()) || 
                (password.equals(getAdminPassword()) && isDefaultPasswordStored)) {
                session.setAttribute("loggedInUser", user);
                System.out.println("Admin login successful");
                return "redirect:/dashboard";
            } else {
                model.addAttribute("error", "Invalid password for admin account");
                return "login";
            }
        }

        // Regular user authentication
        User user = userService.getUserByEmail(email);
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            session.setAttribute("loggedInUser", user);
            System.out.println("User login successful");
            return "redirect:/dashboard";
        } else {
            model.addAttribute("error", "Invalid email or password");
            return "login";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", user);
        model.addAttribute("totalCompanies", companyService.getTotalCompanies());
        model.addAttribute("totalProducts", productService.getTotalProducts());
        model.addAttribute("totalBills", billService.getTotalBills());
        model.addAttribute("totalPayments", paymentService.getTotalPayments());
        model.addAttribute("totalRetailers", retailerService.getTotalRetailers());
        model.addAttribute("totalWholesalers", wholesalerService.getTotalWholesalers());
        model.addAttribute("totalUnits", unitService.getTotalUnits());

        model.addAttribute("latestCompanies", companyService.getLatestCompanies());
        model.addAttribute("latestProducts", productService.getLatestProducts());
        model.addAttribute("latestBills", billService.getLatestBills());
        model.addAttribute("latestPayments", paymentService.getLatestPayments());

        model.addAttribute("latestTransactions", paymentService.getLatestTransactions());
        model.addAttribute("completedTransactions", paymentService.getCompletedTransactionCount());
        model.addAttribute("incompleteTransactions", paymentService.getIncompleteTransactionCount());

        return "dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/edit-profile")
    public String showEditProfilePage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        return "edit-profile";
    }

    @PostMapping("/edit-profile")
    public String editProfile(User user, @RequestParam("photo") MultipartFile photo, HttpSession session) throws IOException {
        if (!photo.isEmpty()) {
            String originalFilename = photo.getOriginalFilename();
            if (originalFilename != null) {
                String fileName = StringUtils.cleanPath(originalFilename);
                String uploadDir = System.getProperty("user.dir") + "/uploads/";
                File uploadDirFile = new File(uploadDir);

                if (!uploadDirFile.exists()) {
                    uploadDirFile.mkdirs();
                }

                File file = new File(uploadDir + fileName);
                photo.transferTo(file);

                user.setPhotoPath("/uploads/" + fileName);
            }
        }

        userService.updateUser(user);
        session.setAttribute("loggedInUser", user);
        return "redirect:/dashboard";
    }
}
