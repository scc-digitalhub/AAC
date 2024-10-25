import {
    Form,
    regex,
    required,
    SaveButton,
    TextInput,
    useTranslate,
} from 'react-admin';
import { DialogActions, Stack } from '@mui/material';
import { Page } from '../components/Page';

const pattern = /^[a-zA-Z.:_-]{3,}$/;

export const AttributeSetCreateForm = () => {
    const translate = useTranslate();

    return (
        <Form>
            <Page>
                <Stack rowGap={2}>
                    <TextInput
                        source="identifier"
                        label="field.identifier.name"
                        helperText="field.identifier.helperText"
                        fullWidth
                        validate={[
                            required(),
                            regex(
                                pattern,
                                translate('ra.validation.regex', {
                                    pattern,
                                })
                            ),
                        ]}
                    />
                    <TextInput
                        source="name"
                        label="field.name.name"
                        helperText="field.name.helperText"
                        fullWidth
                        required
                    />
                </Stack>
            </Page>
            <DialogActions>
                <SaveButton label="ra.action.create" variant="text" />
            </DialogActions>
        </Form>
    );
};
