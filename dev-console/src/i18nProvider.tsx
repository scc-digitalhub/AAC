import polyglotI18nProvider from 'ra-i18n-polyglot';
import englishMessages from 'ra-language-english';
import italianMessages from '@dslab/ra-language-italian';

const customItalian = {
    ...italianMessages,
    resources: {
        apps: {
            fields: {
                name: 'Nome app',
                configuration: {
                    subjectType: 'Tipo soggetto',
                },
            },
        },
    },
};

const customEnglish = {
    ...englishMessages,
    resources: {
        apps: {
            fields: {
                name: 'Name',
                configuration: {
                    subjectType: 'Subject type',
                },
            },
        },
    },
};

console.log(customItalian);

export const i18nProvider = polyglotI18nProvider(
    locale => (locale === 'it' ? customItalian : customEnglish),

    'en', // default locale
    [
        { locale: 'en', name: 'English' },
        { locale: 'it', name: 'Italiano' },
    ],
    { allowMissing: true }
);
