package org.duniter.elasticsearch.user.rest.page;

import com.google.common.html.HtmlEscapers;
import org.duniter.core.exception.BusinessException;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.util.StringUtils;
import org.duniter.elasticsearch.exception.DuniterElasticsearchException;
import org.duniter.elasticsearch.rest.attachment.RestImageAttachmentAction;
import org.duniter.elasticsearch.rest.share.AbstractRestShareLinkAction;
import org.duniter.elasticsearch.user.PluginSettings;
import org.duniter.elasticsearch.user.dao.page.PageIndexDao;
import org.duniter.elasticsearch.user.dao.page.PageRecordDao;
import org.duniter.elasticsearch.user.model.UserProfile;
import org.duniter.elasticsearch.user.model.page.RegistryRecord;
import org.duniter.elasticsearch.user.service.PageService;
import org.duniter.elasticsearch.user.service.UserService;
import org.duniter.elasticsearch.util.opengraph.OGData;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.RestController;
import org.nuiton.i18n.I18n;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

public class RestPageShareLinkAction extends AbstractRestShareLinkAction {

    @Inject
    public RestPageShareLinkAction(final Settings settings, final RestController controller, final Client client,
                                   final PluginSettings pluginSettings,
                                   final PageService service) {
        super(settings, controller, client, PageIndexDao.INDEX, PageRecordDao.TYPE,
                createResolver(pluginSettings, service));
    }

    protected static OGDataResolver createResolver(
            final PluginSettings pluginSettings,
            final PageService service) throws DuniterElasticsearchException, BusinessException {

        return (id) -> {
            try {
                RegistryRecord record = service.getPageForSharing(id);

                OGData data = new OGData();

                if (record != null) {

                    // og:title
                    if (StringUtils.isNotBlank(record.getTitle())) {
                        data.title = record.getTitle();
                    }
                    else {
                        data.title = pluginSettings.getShareSiteName();
                    }

                    // og:description
                    data.description = HtmlEscapers.htmlEscaper().escape(record.getDescription());

                    // og:image
                    if (record.getThumbnail() != null && StringUtils.isNotBlank(record.getThumbnail().get("_content_type"))) {
                        String baseUrl = pluginSettings.getBaseUrl();
                        data.image = StringUtils.isBlank(baseUrl) ? "" : baseUrl;
                        data.image += RestImageAttachmentAction.computeImageUrl(PageIndexDao.INDEX, PageRecordDao.TYPE, id, RegistryRecord.PROPERTY_THUMBNAIL, record.getThumbnail().get("_content_type"));
                    }

                    // og:url
                    data.url = String.format("%s/#/app/page/view/%s/%s",
                            pluginSettings.getCesiumUrl(),
                            id,
                            URLEncoder.encode(record.getTitle(), "UTF-8"));
                }
                else {

                    // og:title
                    data.title = pluginSettings.getShareSiteName();

                    // og:description
                    data.description = I18n.t("duniter.user.share.description");

                    // og:url
                    data.url = String.format("%s/#/app/page/view/%s/%s",
                            pluginSettings.getCesiumUrl(),
                            id,
                            "");
                }

                // og:type
                data.type = "website";

                // og:site_name
                data.siteName = pluginSettings.getShareSiteName();

                // default og:image
                if (StringUtils.isBlank(data.image)) {
                    data.image = pluginSettings.getCesiumUrl() + "/img/logo_128px.png";
                    data.imageType = "image/png";
                }

                return data;
            }
            catch(UnsupportedEncodingException e) {
                throw new TechnicalException(e);
            }
        };
    }
}
