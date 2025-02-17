import {
    Form,
    SaveButton,
    TextInput,
    useInput,
    useRecordContext,
    useTranslate,
} from 'react-admin';
import {
    Box,
    DialogActions,
    Divider,
    FormControl,
    FormGroup,
    FormLabel,
    Grid,
    Stack,
    Switch,
    Typography,
} from '@mui/material';
import { Page } from '../components/Page';
import { authorities } from '../idps/utils';
import { useState } from 'react';

export const DeveloperCreateForm = () => {
    return (
        <Form>
            <Page>
                <Stack rowGap={2}>
                    <TextInput
                        source="subjectId"
                        label="field.subjectId.name"
                        helperText="field.subjectId.helperText"
                        fullWidth
                    />

                    <TextInput
                        source="email"
                        type="email"
                        label="field.email.name"
                        helperText="field.email.helperText"
                        fullWidth
                    />
                </Stack>
            </Page>
            <DialogActions>
                <SaveButton label="ra.action.add" variant="text" />
            </DialogActions>
        </Form>
    );
};

export const DeveloperEditForm = () => {
    const record = useRecordContext();

    if (!record) return null;

    return (
        <Form>
            <Page>
                <AuthoritiesInput />
            </Page>
            <DialogActions>
                <SaveButton label="ra.action.update" variant="text" />
            </DialogActions>
        </Form>
    );
};

export const AuthoritiesInput = () => {
    const translate = useTranslate();
    const record = useRecordContext();
    const { field } = useInput({
        source: 'authorities',
    });

    const value: any[] = field?.value || [];

    const handleSwitch = role => {
        return (event: React.ChangeEvent<HTMLInputElement>) => {
            const keep = value.filter(e => e.role !== role);
            const edit = event.target.checked
                ? [{ realm: record.realm, role: role }]
                : [];
            const res = [...keep, ...edit];

            field.onChange(res);
        };
    };

    //hard-coded
    const roles = ['ROLE_DEVELOPER', 'ROLE_ADMIN'];

    return (
        <FormControl component="fieldset" fullWidth>
            <FormLabel component="legend" sx={{ mb: 2 }}>
                {translate('page.authorities.helperText')}
            </FormLabel>
            <FormGroup>
                {roles.map(r => {
                    const checked = value.find(a => a.role === r) || false;

                    return (
                        <Box key={r} mb={2}>
                            <Grid container mb={1}>
                                <Grid item xs={10}>
                                    <Stack rowGap={1}>
                                        <Typography variant="h6">
                                            {translate(
                                                'authorities.' + r + '.name'
                                            )}
                                        </Typography>
                                        <Typography
                                            variant="body2"
                                            fontFamily={'monospace'}
                                        >
                                            {r}
                                        </Typography>
                                        <Typography variant="body2">
                                            {translate(
                                                'authorities.' +
                                                    r +
                                                    '.description'
                                            )}
                                        </Typography>
                                    </Stack>
                                </Grid>
                                <Grid item xs={2}>
                                    <Switch
                                        checked={checked}
                                        onChange={handleSwitch(r)}
                                        name={r}
                                    />
                                </Grid>
                            </Grid>
                            <Divider />
                        </Box>
                    );
                })}
            </FormGroup>
        </FormControl>
    );
};
