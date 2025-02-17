import { TranslationMessages } from 'ra-core';
import raMessages from 'ra-language-english';
import utils from '../../utils';
import resources from './resources';
import * as fields from './fields.json';
import * as messages from './messages.json';
import * as errors from './errors.json';
import * as pages from './pages.json';
import * as tabs from './tabs.json';
import * as actions from './actions.json';
import * as authorities from './authorities.json';

import translations from './translations';

const englishMessages: TranslationMessages = {
    ra: utils.deepCopy(raMessages.ra),
    resources: resources,
    field: fields,
    message: messages,
    error: errors,
    page: pages,
    tab: tabs,
    action: actions,
    authority: authorities,
    ...translations,
};

export default englishMessages;
