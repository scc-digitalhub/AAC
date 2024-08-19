import {
    EditInDialogButton,
    ShowInDialogButton,
    useDialogContext,
} from '@dslab/ra-dialog-crud';
import {
    Stack,
    Box,
    Divider,
    TableCell,
    TableHead,
    TableRow,
    DialogActions,
} from '@mui/material';
import {
    Datagrid,
    ArrayField,
    SingleFieldList,
    ChipField,
    TextField,
    DatagridHeaderProps,
    FieldProps,
    CheckboxGroupInput,
    ReferenceArrayInput,
    FunctionField,
    WrapperField,
    Form,
    SaveButton,
    Button,
    useTranslate,
    useResourceContext,
    useInput,
    useRecordContext,
} from 'react-admin';
import { useWatch, ControllerRenderProps } from 'react-hook-form';
import { IdField } from '../components/IdField';
import { NameField } from '../components/NameField';
import { Page } from '../components/Page';
import React, { useMemo } from 'react';
import ConfirmIcon from '@mui/icons-material/CheckCircle';
import { DataGridBlankHeader } from '../components/DataGridBlankHeader';


export const AppResources = () => {
    const resource = useResourceContext();
    const { field } = useInput({
        resource,
        source: 'scopes',
    });

    return (
        <Datagrid
            bulkActionButtons={false}
            rowClick={false}
            header={<DataGridBlankHeader />}
        >
            <NameField
                text="name"
                secondaryText="description"
                source="name"
                icon={false}
            />
            <IdField source="resourceId" label="resource" />

            <ShowInDialogButton variant="contained" maxWidth={'md'} fullWidth>
                <Form>
                    <AppResourcesDialog field={field} />
                </Form>
            </ShowInDialogButton>
        </Datagrid>
    );
};
export const AppResourcesDialog = (props: { field: ControllerRenderProps }) => {
    const { field } = props;
    const translate = useTranslate();
    const { handleClose } = useDialogContext();
    const record = useRecordContext();

    const defaultValue = useMemo(() => {
        if (record) {
            return field?.value?.filter(i =>
                record.scopes.map(s => s['id']).includes(i)
            );
        }
        return [];
    }, [record]);
    const selected = useWatch({ name: 'selected', defaultValue });

    const handleUpdate = e => {
        const scopes: any[] = [];
        record?.scopes?.forEach(s => {
            scopes.push({ id: s['id'], selected: selected.includes(s['id']) });
        });
        const value = [...field.value].filter(
            v => scopes.find(s => v === s['id']) === undefined
        );

        value.push(...scopes.filter(s => s['selected']).map(s => s['id']));
        field.onChange(value);

        handleClose(e);
    };

    return (
        <>
            <Page>
                <Stack direction={'column'}>
                    <TextField
                        source="description"
                        variant="h6"
                        sx={{ mb: 1 }}
                    />

                    <CheckboxGroupInput
                        row={false}
                        source="selected"
                        label={false}
                        choices={record.scopes}
                        labelPlacement="end"
                        defaultValue={defaultValue}
                        optionText={
                            <NameField
                                text="name"
                                secondaryText={
                                    <WrapperField>
                                        <TextField source="scope" />
                                        <TextField source="authorities" />
                                    </WrapperField>
                                }
                                tertiaryText="description"
                                source="name"
                                icon={false}
                            />
                        }
                        sx={{ textAlign: 'left' }}
                    />
                </Stack>
            </Page>
            <DialogActions>
                <Button
                    onClick={handleUpdate}
                    autoFocus
                    startIcon={<ConfirmIcon />}
                    label={translate('ra.action.update')}
                />
            </DialogActions>
        </>
    );
};
