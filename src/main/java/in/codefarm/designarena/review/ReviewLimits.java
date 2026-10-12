package in.codefarm.designarena.review;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * How much reviewing a plan is entitled to.
 *
 * <p>A design review is the expensive thing this application does — a minute of a model's
 * time, and real money on a paid provider — so the free plan has a monthly cap. The
 * number lives in configuration because it is a commercial decision, not an engineering
 * one, and it will be changed by somebody who is not reading this code.
 *
 * @param freeReviewsPerMonth how many design reviews a free account may run in a calendar
 *     month. Paid accounts are not capped.
 */
@ConfigurationProperties("designarena.limits")
public record ReviewLimits(@DefaultValue("3") int freeReviewsPerMonth) {}
