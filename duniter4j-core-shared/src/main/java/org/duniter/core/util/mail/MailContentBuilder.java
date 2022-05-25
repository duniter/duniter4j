package org.duniter.core.util.mail;

import com.google.common.collect.Lists;
import org.duniter.core.util.CollectionUtils;
import org.duniter.core.util.Preconditions;
import org.duniter.core.util.StringUtils;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.URLDataSource;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by StrongMan on 25/05/14.
 * See https://stackoverflow.com/questions/3902455/mail-multipart-alternative-vs-multipart-mixed
 */
public class MailContentBuilder {

    private static final Pattern COMPILED_PATTERN_SRC_URL_SINGLE = Pattern.compile("src='([^']*)'",  Pattern.CASE_INSENSITIVE);
    private static final Pattern COMPILED_PATTERN_SRC_URL_DOUBLE = Pattern.compile("src=\"([^\"]*)\"",  Pattern.CASE_INSENSITIVE);


    public static List<URL> findUrls(String messageHtml) {
        Preconditions.checkNotNull(messageHtml);
        List<URL> urls = Lists.newArrayList();
        urls.addAll(findUrls(messageHtml, COMPILED_PATTERN_SRC_URL_SINGLE));
        urls.addAll(findUrls(messageHtml, COMPILED_PATTERN_SRC_URL_DOUBLE));
        return urls;
    }

    /**
     * Build an email message.
     *
     * The HTML may reference the embedded image (messageHtmlInline) using the filename. Any path portion is ignored to make my life easier
     * e.g. If you pass in the image C:\Temp\dog.jpg you can use <img src="dog.jpg"/> or <img src="C:\Temp\dog.jpg"/> and both will work
     *
     * @param messageText
     * @param messageHtml
     * @param embeddedImages
     * @param attachments
     * @return
     * @throws MessagingException
     */
    public Multipart build(String messageText, String messageHtml, List<URL> embeddedImages, List<URL> attachments) throws MessagingException {
        final Multipart mpMixed = new MimeMultipart("mixed");
        {
            // alternative
            final Multipart mpMixedAlternative = newChild(mpMixed, "alternative");
            {
                // Note: MUST RENDER HTML LAST otherwise iPad mail client only renders the last image and no email
                addTextVersion(mpMixedAlternative,messageText);
                addHtmlVersion(mpMixedAlternative,messageHtml, embeddedImages);
            }
            // attachments
            addAttachments(mpMixed,attachments);
        }

        //msg.setText(message, "utf-8");
        //msg.setContent(message,"text/html; charset=utf-8");
        return mpMixed;
    }

    private Multipart newChild(Multipart parent, String alternative) throws MessagingException {
        MimeMultipart child =  new MimeMultipart(alternative);
        final MimeBodyPart mbp = new MimeBodyPart();
        parent.addBodyPart(mbp);
        mbp.setContent(child);
        return child;
    }

    private void addTextVersion(Multipart mpRelatedAlternative, String messageText) throws MessagingException {
        final MimeBodyPart textPart = new MimeBodyPart();
        textPart.setContent(messageText, "text/plain");
        mpRelatedAlternative.addBodyPart(textPart);
    }

    private void addHtmlVersion(Multipart parent, String messageHtml, List<URL> embedded) throws MessagingException {
        // HTML version
        final Multipart mpRelated = newChild(parent,"related");

        // Get embedded URL from message
        if (embedded == null) {
            embedded = findUrls(messageHtml);
        }

        // Html
        final MimeBodyPart htmlPart = new MimeBodyPart();
        HashMap<String,String> cids = new HashMap<String, String>();
        htmlPart.setContent(replaceUrlWithCids(messageHtml,cids), "text/html");
        mpRelated.addBodyPart(htmlPart);

        // Inline images
        addImagesInline(mpRelated, embedded, cids);
    }

    private void addImagesInline(Multipart parent, List<URL> embedded, HashMap<String,String> cids) throws MessagingException {
        if (embedded != null) {
            for (URL img : embedded) {
                final MimeBodyPart htmlPartImg = new MimeBodyPart();
                DataSource htmlPartImgDs = new URLDataSource(img);
                htmlPartImg.setDataHandler(new DataHandler(htmlPartImgDs));
                String fileName = img.getFile();
                fileName = getFileName(fileName);
                String newFileName = cids.get(fileName);
                boolean imageNotReferencedInHtml = newFileName == null;
                if (imageNotReferencedInHtml) continue;
                // Gmail requires the cid have <> around it
                htmlPartImg.setHeader("Content-ID", "<"+newFileName+">");
                htmlPartImg.setDisposition(BodyPart.INLINE);
                parent.addBodyPart(htmlPartImg);
            }
        }
    }

    private void addAttachments(Multipart parent, List<URL> attachments) throws MessagingException {
        if (attachments != null)
        {
            for (URL attachment : attachments)
            {
                final MimeBodyPart mbpAttachment = new MimeBodyPart();
                DataSource htmlPartImgDs = new URLDataSource(attachment);
                mbpAttachment.setDataHandler(new DataHandler(htmlPartImgDs));
                String fileName = attachment.getFile();
                fileName = getFileName(fileName);
                mbpAttachment.setDisposition(BodyPart.ATTACHMENT);
                mbpAttachment.setFileName(fileName);
                parent.addBodyPart(mbpAttachment);
            }
        }
    }

    public String replaceUrlWithCids(String html, HashMap<String,String> cids)
    {
        html = replaceUrlWithCids(html, COMPILED_PATTERN_SRC_URL_SINGLE, "src='cid:@cid'", cids);
        html = replaceUrlWithCids(html, COMPILED_PATTERN_SRC_URL_DOUBLE, "src=\"cid:@cid\"", cids);
        return html;
    }

    private String replaceUrlWithCids(String html, Pattern pattern, String replacement, HashMap<String,String> cids) {
        Matcher matcher = pattern.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String fileName = matcher.group(1);
            // Disregarding file path, so don't clash your filenames!
            fileName = getFileName(fileName);
            // A cid must start with @ and be globally unique
            String cid = "@" + UUID.randomUUID() + "_" + fileName;
            if (cids.containsKey(fileName))
                cid = cids.get(fileName);
            else
                cids.put(fileName,cid);
            matcher.appendReplacement(sb,replacement.replace("@cid",cid));
        }
        matcher.appendTail(sb);
        html = sb.toString();
        return html;
    }

    private String getFileName(String fileName) {
        if (fileName.contains("/"))
            fileName = fileName.substring(fileName.lastIndexOf("/")+1);
        return fileName;
    }


    private static List<URL> findUrls(String messageHtml, Pattern pattern) {
        List<URL> urls = Lists.newArrayList();
        Matcher matcher = pattern.matcher(messageHtml);
        while (matcher.find()) {
            String src = matcher.group(1);
            try {
                URL url = new URL(src);
                urls.add(url);
            } catch (MalformedURLException e) {
                // Skip
            }
        }
        return urls;
    }
}