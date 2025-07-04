package com.DM.dairyManagement.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.DM.dairyManagement.model.Bill;
import com.DM.dairyManagement.service.BillService;
import com.DM.dairyManagement.repository.BillRepository;
import com.DM.dairyManagement.repository.ProductRepository;
import com.DM.dairyManagement.repository.StockRepository;

@Controller
public class DashboardController {

    @Autowired
    private BillService billService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BillRepository billRepository;
    
    @Autowired
    private StockRepository stockRepository;

    @GetMapping("/createBill")
    public String showBillForm(Model model) {
        model.addAttribute("bill", new Bill());
        model.addAttribute("products", productRepository.findAll());
        return "createBill";
    }

    @PostMapping("/createBill")
    public String saveBill(@ModelAttribute Bill bill, 
                          @RequestParam(value = "item", required = false) String[] items,
                          @RequestParam(value = "qty", required = false) Integer[] quantities,
                          @RequestParam(value = "price", required = false) Double[] prices,
                          @RequestParam(value = "subtotal", required = false) Double[] subtotals,
                          RedirectAttributes redirectAttributes) {
        
        System.out.println("Bill received: " + bill);
        System.out.println("Items: " + (items != null ? items.length : "null"));
        System.out.println("Quantities: " + (quantities != null ? quantities.length : "null"));
        System.out.println("Prices: " + (prices != null ? prices.length : "null"));
        
        // If arrays are null but bill has item, qty and price, use those values
        if ((items == null || items.length == 0) && bill.getItem() != null) {
            // The form is submitting single values, not arrays
            System.out.println("Using single values from bill object");
            try {
                // Use the BillService to handle both bill creation and stock update
                billService.saveBill(bill);
                redirectAttributes.addFlashAttribute("success", "Bill saved successfully and stock updated!");
                return "redirect:/listBill";
            } catch (IllegalArgumentException e) {
                // Handle specific exceptions from the service
                System.out.println("IllegalArgumentException: " + e.getMessage());
                redirectAttributes.addFlashAttribute("error", e.getMessage());
                return "redirect:/createBill";
            } catch (Exception e) {
                // Handle any other unexpected errors
                System.out.println("Exception: " + e.getMessage());
                e.printStackTrace();
                redirectAttributes.addFlashAttribute("error", "An error occurred: " + e.getMessage());
                return "redirect:/createBill";
            }
        }
        
        // Check if we have at least one product in the arrays
        if (items == null || items.length == 0 || quantities == null || quantities.length == 0 || 
            prices == null || prices.length == 0) {
            System.out.println("No products found in arrays");
            redirectAttributes.addFlashAttribute("error", "At least one product must be added to the bill");
            return "redirect:/createBill";
        }
        
        try {
            System.out.println("Using first product from arrays");
            // For now, we'll just use the first product in the arrays
            // In a real multi-product solution, you'd create multiple bill entries or a different data model
            bill.setItem(items[0]);
            bill.setQty(quantities[0]);
            bill.setPrice(prices[0]);
            if (subtotals != null && subtotals.length > 0) {
                bill.setSubtotal(subtotals[0]);
            }
            
            // Use the BillService to handle both bill creation and stock update
            billService.saveBill(bill);
            redirectAttributes.addFlashAttribute("success", "Bill saved successfully and stock updated!");
            return "redirect:/listBill";
        } catch (IllegalArgumentException e) {
            // Handle specific exceptions from the service
            System.out.println("IllegalArgumentException: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/createBill";
        } catch (Exception e) {
            // Handle any other unexpected errors
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "An error occurred: " + e.getMessage());
            return "redirect:/createBill";
        }
    }

    @PostMapping("/update-bill")
    public String updateBill(@ModelAttribute Bill bill, RedirectAttributes redirectAttributes) {
        if (Objects.isNull(bill.getQty()) || Objects.isNull(bill.getPrice())) {
            redirectAttributes.addFlashAttribute("error", "Quantity and Price cannot be empty");
            return "redirect:/edit-bill/" + bill.getId();
        }

        try {
            // Calculate fields and save the bill
            double subtotal = bill.getQty() * bill.getPrice();
            bill.setSubtotal(roundToTwoDecimalPlaces(subtotal));

            double cgst = Objects.nonNull(bill.getCgst()) ? bill.getCgst() : 0;
            double sgst = Objects.nonNull(bill.getSgst()) ? bill.getSgst() : 0;
            double discount = Objects.nonNull(bill.getDiscount()) ? bill.getDiscount() : 0;
            double paidAmount = Objects.nonNull(bill.getPaidAmount()) ? bill.getPaidAmount() : 0;

            double cgstAmount = (cgst * subtotal) / 100;
            double sgstAmount = (sgst * subtotal) / 100;
            double total = subtotal + cgstAmount + sgstAmount - discount;
            double balanceDue = total - paidAmount;

            bill.setTotal(roundToTwoDecimalPlaces(total));
            bill.setBalanceDue(roundToTwoDecimalPlaces(balanceDue));
            
            billRepository.save(bill);
            redirectAttributes.addFlashAttribute("success", "Bill updated successfully!");
            return "redirect:/listBill";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating bill: " + e.getMessage());
            return "redirect:/edit-bill/" + bill.getId();
        }
    }

    private double roundToTwoDecimalPlaces(Double value) {
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    @GetMapping("/listBill")
    public String viewBills(Model model) {
        List<Bill> bills = billService.getAllBills();
        model.addAttribute("bills", bills);
        return "listBill";
    }

    @GetMapping("/PrintBill/{id}")
    public String printBill(@PathVariable Long id, Model model) {
        Bill bill = billService.getBillById(id);
        model.addAttribute("bill", bill);
        return "printBill";
    }

    @GetMapping("/next-bill-no")
    @ResponseBody
    public Long getNextBillNo() {
        return billService.getNextBillNo();
    }

    @PostMapping("/delete-bill/{id}")
    public String deleteBill(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            billService.deleteBillById(id);
            redirectAttributes.addFlashAttribute("success", "Bill deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting bill: " + e.getMessage());
        }
        return "redirect:/listBill";
    }

    @GetMapping("/edit-bill/{id}")
    public String editBill(@PathVariable Long id, Model model) {
        Bill bill = billService.getBillById(id);
        model.addAttribute("bill", bill);
        model.addAttribute("products", productRepository.findAll());
        return "editBill";
    }

    @GetMapping("/customers")
    public String showCustomerBills(Model model) {
        List<Bill> customerBills = billService.getBillsByType("customer");
        model.addAttribute("bills", customerBills);
        return "customer-bills";
    }

    @GetMapping("/retailers")
    public String showRetailerBills(Model model) {
        List<Bill> retailerBills = billService.getBillsByType("retailer");
        model.addAttribute("bills", retailerBills);
        return "retailer-bills";
    }

    @GetMapping("/wholesalers")
    public String showWholesalerBills(Model model) {
        List<Bill> wholesalerBills = billService.getBillsByType("wholesaler");
        model.addAttribute("bills", wholesalerBills);
        return "wholesaler-bills";
    }
}
