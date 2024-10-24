import {
    Form,
    SaveButton,
    SelectInput,
    TextInput,
    useDataProvider,
} from 'react-admin';
import { DialogActions, Stack } from '@mui/material';
import { Page } from '../components/Page';
import { useEffect, useMemo, useState } from 'react';
import { useRootSelector } from '@dslab/ra-root-selector';

export const ApCreateForm = () => {
    const dataProvider = useDataProvider();
    const { root: realmId } = useRootSelector();
    const [authorities, setAuthorities] = useState<string[]>([]);

    useEffect(() => {
        if (dataProvider) {
            dataProvider
                .invoke({ path: 'aps/' + realmId + '/authorities' })
                .then(data => {
                    setAuthorities(data || []);
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
                            name: 'authority.' + a,
                        }))}
                    />
                    <TextInput
                        source="name"
                        label="field.name.name"
                        helperText="field.name.helperText"
                        fullWidth
                    />
                </Stack>
            </Page>
            <DialogActions>
                <SaveButton label="ra.action.create" variant="text" />
            </DialogActions>
        </Form>
    );
};
