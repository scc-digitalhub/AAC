import polyglotI18nProvider from 'ra-i18n-polyglot';
import { TranslationMessages } from 'react-admin';

import italianMessages from './i18n/it';
import germanMessages from './i18n/de';
import englishMessages from './i18n/en';

const messages: Record<string, TranslationMessages> = {
    en: englishMessages,
    it: italianMessages,
    de: germanMessages,
};

export default polyglotI18nProvider(
    (locale: string) => {
        return messages[locale];
    },
    'en',
    [
        { locale: 'en', name: 'EN' },
        { locale: 'it', name: 'IT' },
        { locale: 'de', name: 'DE' },
    ]
);
