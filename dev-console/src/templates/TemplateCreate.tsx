import {
    Form,
    FormDataConsumer,
    SaveButton,
    SelectInput,
    TextInput,
    useDataProvider,
} from 'react-admin';
import { DialogActions, Stack } from '@mui/material';
import { Page } from '../components/Page';
import { useEffect, useMemo, useState } from 'react';
import { useRootSelector } from '@dslab/ra-root-selector';
import { DEFAULT_LANGUAGES } from '../App';
import { useWatch } from 'react-hook-form';
import dataProvider from '../dataProvider';

const TemplateInput = () => {
    const dataProvider = useDataProvider();
    const { root: realmId } = useRootSelector();
    const authority = useWatch({ name: 'authority' });
    const [choices, setChoices] = useState<any[]>([]);

    useEffect(() => {
        if (dataProvider && realmId && authority) {
            dataProvider
                .invoke({ path: 'templates/' + realmId + '/' + authority })
                .then(data => {
                    setChoices(
                        data.map(t => ({
                            id: t.template,
                            name: t.template,
                        }))
                    );
                })
                .catch(error => {
                    setChoices([]);
                });
        }
    }, [authority, dataProvider, realmId]);

    return (
        <SelectInput
            source="template"
            required
            label="field.template.name"
            helperText="field.template.helperText"
            choices={choices}
        />
    );
};

export const TemplateCreateForm = () => {
    const dataProvider = useDataProvider();
    const { root: realmId } = useRootSelector();
    const [authorities, setAuthorities] = useState<string[]>([]);
    const [availableLocales, setAvailableLocales] =
        useState<string[]>(DEFAULT_LANGUAGES);

    useEffect(() => {
        if (dataProvider && realmId) {
            dataProvider.invoke({ path: 'templates/' + realmId }).then(data => {
                setAuthorities(data || []);
            });

            dataProvider.getOne('myrealms', { id: realmId }).then(data => {
                if (data && 'localizationConfiguration' in data) {
                    setAvailableLocales(
                        (data.localizationConfiguration as any)['languages'] ||
                            DEFAULT_LANGUAGES
                    );
                }
            });
        }
    }, [dataProvider]);

    return (
        <Form>
            <Page>
                <Stack rowGap={2}>
                    <SelectInput
                        source="authority"
                        required
                        label="field.authority.name"
                        helperText="field.authority.helperText"
                        choices={authorities.sort().map(a => ({
                            id: a,
                            name: a,
                        }))}
                    />
                    <TemplateInput />
                    <SelectInput
                        source="language"
                        required
                        label="field.language.name"
                        helperText="field.language.helperText"
                        choices={availableLocales.sort().map(a => ({
                            id: a,
                            name: a,
                        }))}
                    />
                </Stack>
            </Page>
            <DialogActions>
                <SaveButton label="ra.action.create" variant="text" />
            </DialogActions>
        </Form>
    );
};
