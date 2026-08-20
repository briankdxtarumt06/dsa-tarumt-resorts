/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarumtresort.utility;

import java.util.Scanner;

// Author: Brian Kam Ding Xian
public class SharedServices {
       
    public static String askNonEmptyInput(Scanner scanner, String prompt) {
        String userInput;
        boolean validInput;

        do {
            System.out.print(prompt + ": ");
            userInput = scanner.nextLine().trim();

            if (userInput.isEmpty()) {
                validInput = false;
                System.out.println("Error: Input cannot be empty! Please try again.");
            } else {
                validInput = true;
            }
        } while (!validInput);

        return userInput;
    }
}
