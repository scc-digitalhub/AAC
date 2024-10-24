import polyglotI18nProvider from 'ra-i18n-polyglot';
import { TranslationMessages } from 'react-admin';
import englishMessages from './i18n/en';

const messages: Record<string, TranslationMessages> = {
    en: englishMessages,
};

export default polyglotI18nProvider(
    (locale: string) => {
        return messages[locale];
    },
    'en',
    [{ locale: 'en', name: 'EN' }]
);
