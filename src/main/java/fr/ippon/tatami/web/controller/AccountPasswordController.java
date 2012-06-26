package fr.ippon.tatami.web.controller;

import fr.ippon.tatami.domain.User;
import fr.ippon.tatami.security.AuthenticationService;
import fr.ippon.tatami.service.UserService;
import fr.ippon.tatami.web.controller.form.UserPassword;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.inject.Inject;
import javax.validation.ConstraintViolationException;

/**
 * @author Julien Dubois
 */
@Controller
public class AccountPasswordController {

    private final Log log = LogFactory.getLog(AccountPasswordController.class);

    @Inject
    private UserService userService;

    @Inject
    private AuthenticationService authenticationService;

    @ModelAttribute("user")
    public User initUser() {
        User currentUser = authenticationService.getCurrentUser();
        return userService.getUserByLogin(currentUser.getLogin());
    }

    @ModelAttribute("userPassword")
    public UserPassword formBackingObject() {
        return new UserPassword();
    }

    @RequestMapping(value = "/account/password",
            method = RequestMethod.GET)
    public ModelAndView getUpdatePassword(@RequestParam(required = false) boolean success) {
        ModelAndView mv = new ModelAndView("account/password");
        mv.addObject("success", success);
        return mv;
    }

    @RequestMapping(value = "/account/password",
            method = RequestMethod.POST)
    public ModelAndView updatePassword(@ModelAttribute("userPassword")
                                       UserPassword userPassword, BindingResult result) {

        User currentUser = authenticationService.getCurrentUser();
        if (!currentUser.getPassword().equals(userPassword.getOldPassword())) {
            if (log.isDebugEnabled()) {
                log.debug("The old password is incorrect : " + userPassword.getOldPassword());
            }
            result.rejectValue("oldPassword",
                    "tatami.user.old.password.error",
                    "The old password is incorrect");
        }
        if (!userPassword.getNewPassword().equals(userPassword.getNewPasswordConfirmation())) {
            result.rejectValue("newPasswordConfirmation",
                    "tatami.user.new.password.confirmation.error",
                    "The new password confirmation is incorrect");
        }
        if (result.hasErrors()) {
            log.debug("Errors : " + result.getErrorCount());
            for (ObjectError error : result.getAllErrors()) {
                log.debug("Error=" + error.toString());
            }
            ModelAndView mv = new ModelAndView("account/password");
            mv.addAllObjects(result.getModel());
            log.debug("mv:="+mv);
            return mv;
        }

        currentUser.setPassword(userPassword.getNewPassword());
        try {
            userService.updateUser(currentUser);
        } catch (ConstraintViolationException cve) {
            result.reject(cve.getMessage(), "The new password is not valid : " + cve.getMessage());
            return getUpdatePassword(false);
        }
        if (log.isDebugEnabled()) {
            log.debug("User password updated : " + currentUser);
        }
        ModelAndView mv = new ModelAndView("redirect:/tatami/account/password?success=true");

        return mv;
    }
}
