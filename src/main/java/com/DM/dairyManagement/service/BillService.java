package com.DM.dairyManagement.service;

import com.DM.dairyManagement.model.Bill;
import com.DM.dairyManagement.model.Stock;
import com.DM.dairyManagement.repository.BillRepository;
import com.DM.dairyManagement.repository.ProductRepository;
import com.DM.dairyManagement.repository.StockRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class BillService {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BillRepository billRepository;

    // ✅ Save Bill + Deduct Stock
    @Transactional
    public void saveBill(Bill bill) {
        // 🧮 Basic calculations
        double qty = Optional.ofNullable(bill.getQty()).map(Integer::doubleValue).orElse(0.0);
        double price = Optional.ofNullable(bill.getPrice()).orElse(0.0);
        double cgst = Optional.ofNullable(bill.getCgst()).orElse(0.0);
        double sgst = Optional.ofNullable(bill.getSgst()).orElse(0.0);
        double discount = Optional.ofNullable(bill.getDiscount()).orElse(0.0);
        double paidAmount = Optional.ofNullable(bill.getPaidAmount()).orElse(0.0);

        double subtotal = qty * price;
        bill.setSubtotal(subtotal);

        double cgstAmount = (subtotal * cgst) / 100;
        double sgstAmount = (subtotal * sgst) / 100;
        discount = Math.min(discount, subtotal); // to avoid negative totals

        double total = subtotal + cgstAmount + sgstAmount - discount;
        bill.setTotal(total);

        double balanceDue = Math.max(0, total - paidAmount);
        bill.setBalanceDue(balanceDue);

        // 📦 Deduct stock for the billed product
        if (bill.getItem() == null || bill.getItem().trim().isEmpty()) {
            throw new IllegalArgumentException("❌ Product name cannot be empty");
        }
        
        String itemName = bill.getItem().trim(); // ✅ FIXED HERE
        int qtyToDeduct = bill.getQty();  // Quantity being billed
        
        System.out.println("Looking for stock for product: " + itemName);
        
        try {
            // Fetch all stocks for this product name (case-insensitive, trimmed)
            List<Stock> stocks = stockRepository.findAllStocksByProductNameIgnoreCase(itemName);
            
            System.out.println("Found " + stocks.size() + " stock entries for product: " + itemName);
            
            if (stocks.isEmpty()) {
                throw new IllegalArgumentException("❌ Stock not found for product: " + itemName);
            }
            
            // Use the first stock entry (we could implement more complex logic if needed)
            Stock stock = stocks.get(0);
            int currentQty = stock.getQuantity();
            
            System.out.println("Current stock quantity: " + currentQty + ", Quantity to deduct: " + qtyToDeduct);
    
            if (currentQty < qtyToDeduct) {
                throw new IllegalArgumentException("❌ Insufficient stock for product: " + itemName +
                                                   ". Available: " + currentQty + ", Required: " + qtyToDeduct);
            }
    
            // Deduct and save updated quantity
            stock.setQuantity(currentQty - qtyToDeduct);
            stockRepository.save(stock); // ✅ Persist updated stock
            System.out.println("Stock updated successfully. New quantity: " + stock.getQuantity());
        } catch (Exception e) {
            System.out.println("Error updating stock: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw to be handled by the controller
        }

        // 💾 Finally, save the bill
        billRepository.save(bill);
        System.out.println("Product Name: " + itemName);
        System.out.println("Quantity to Deduct: " + qtyToDeduct);
        System.out.println("Bill saved successfully!");

    }

    // ✅ Dashboard stats support methods
    public long getTotalBills() {
        return billRepository.count();
    }

    public List<Bill> getBillsByType(String customerType) {
        return billRepository.findByCustomerType(customerType);
    }

    public List<Bill> getAllBills() {
        return billRepository.findAll();
    }

    public List<Bill> getLatestBills() {
        return billRepository.findTop5ByOrderByIdDesc();
    }

    public Bill getBillById(Long id) {
        return billRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bill with ID " + id + " not found!"));
    }

    public void deleteBillById(Long id) {
        if (!billRepository.existsById(id)) {
            throw new IllegalArgumentException("Bill with ID " + id + " does not exist!");
        }
        billRepository.deleteById(id);
    }

    public Long getNextBillNo() {
        return billRepository.findNextBillNo();
    }

    public List<Bill> findBillByCustomerName(String customerName) {
        return billRepository.findByFullName(customerName);
    }
}
