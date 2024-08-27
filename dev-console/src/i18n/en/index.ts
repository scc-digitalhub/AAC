import { TranslationMessages } from 'ra-core';
import raMessages from 'ra-language-english';
import utils from '../../utils';
import resources from './resources';
import fields from './fields';
import messages from './messages';
import errors from './errors';
import { pages, tabs } from './pages';
import translations from './translations';
import actions from './actions';

const englishMessages: TranslationMessages = {
    ra: utils.deepCopy(raMessages.ra),
    resources: resources,
    field: fields,
    message: messages,
    error: errors,
    page: pages,
    tab: tabs,
    action: actions,
    ...translations,
};

export default englishMessages;
