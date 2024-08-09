import {
    BooleanInput,
    Form,
    minLength,
    SaveButton,
    TextInput,
} from 'react-admin';
import { DialogActions, Stack } from '@mui/material';
import { Page } from '../components/Page';

export const RealmCreateForm = () => {
    return (
        <Form>
            <Page>
                <Stack rowGap={2}>
                    <TextInput
                        source="slug"
                        label="field.slug.name"
                        helperText="field.slug.helperText"
                        fullWidth
                        required
                        validate={[minLength(3, 'error.message.min_length')]}
                    />

                    <TextInput
                        source="name"
                        label="field.name.name"
                        helperText="field.name.helperText"
                        fullWidth
                        required
                        validate={[minLength(3, 'error.message.min_length')]}
                    />

                    <BooleanInput
                        source="public"
                        label="field.public.name"
                        helperText="field.public.helperText"
                        defaultValue={true}
                    />
                </Stack>
            </Page>
            <DialogActions>
                <SaveButton variant="text" />
            </DialogActions>
        </Form>
    );
};
