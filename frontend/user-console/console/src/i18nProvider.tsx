import polyglotI18nProvider from 'ra-i18n-polyglot';
import { TranslationMessages } from 'react-admin';
import englishMessages from './i18n/en';
import italianMessages from './i18n/it';
import germanMessages from './i18n/de';
import spanishMessages from './i18n/en';
import latvianMessages from './i18n/en';

const messages: Record<string, TranslationMessages> = {
    en: englishMessages,
    it: italianMessages,
    de: germanMessages,
    es: spanishMessages,
    lv: latvianMessages,
};

export default polyglotI18nProvider(
    (locale: string) => {
        return messages[locale];
    },
    'en',
    [
        { locale: 'en', name: 'EN' },
        { locale: 'it', name: 'IT' },
        // { locale: 'lv', name: 'LV' },
        // { locale: 'es', name: 'ES' },
        { locale: 'de', name: 'DE' },
    ]
);
