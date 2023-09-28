import {
    Button,
    SimpleForm,
    TextInput,
    required,
    useDelete,
    useNotify,
    useRecordContext,
    useRedirect,
    useRefresh,
    useStore,
} from 'react-admin';
import DeleteIcon from '@mui/icons-material/Delete';
import { CustomDeleteConfirm } from './CustomDeleteConfirm';
import React from 'react';
/**
 * Custom Delete button dialog
 *
 * @example
 * <Confirm
 *     isOpen={true}
 *     title="Item title"
 *     resourceName="Item name"
 *     registeredResource="resource id registered in Apps.tsx file"
 *     redirectUrl="url to redirect after delete"
 * />
 */
export const CustomDeleteButtonDialog = (params: any) => {
    const redirect = useRedirect();
    const record = useRecordContext();
    const notify = useNotify();
    const refresh = useRefresh();
    const [open, setOpen] = React.useState(false);
    const [disabledeletebutton, setDisableDeleteButton] =
        useStore('delete.disabled');

    const handleClick = () => {
        setDisableDeleteButton(true);
        setOpen(true);
    };

    const [deleteOne, { isLoading }] = useDelete(
        params.registeredResource,
        { id: record.id, meta: { realmId: params.realmId } },
        {
            onSuccess: () => {
                notify(params.resourceName + ` deleted successfully`);
                redirect(params.redirectUrl);
            },
        }
    );

    let title = params.title;

    const handleDialogClose = () => setOpen(false);
    const handleConfirm = (data: any) => {
        deleteOne();
        setOpen(false);
    };

    return (
        <>
            <span>
                <Button
                    label="Delete"
                    onClick={handleClick}
                    sx={{ color: 'red' }}
                >
                    {<DeleteIcon />}
                </Button>
            </span>
            <CustomDeleteConfirm
                disableDeleteButton={disabledeletebutton}
                isOpen={open}
                loading={isLoading}
                title={title}
                content={
                    <DialogContent
                        id={record.id}
                        resourceName={params.resourceName}
                    />
                }
                onDelete={handleConfirm}
                onClose={handleDialogClose}
                onTouchCancel={handleDialogClose}
            />
        </>
    );
};

const DialogContent = (params: any) => {
    const [disabledeletebutton, setDisableDeleteButton] =
        useStore('delete.disabled');

    const idValidation = (value: any, allValues: any) => {
        if (value === params.id) {
            setDisableDeleteButton(false);
        } else {
            setDisableDeleteButton(true);
        }
        if (!value) {
            return 'The id is required';
        }
        if (value !== params.id) {
            return 'Id does not match';
        }
        return undefined;
    };

    const validateClientId = [required(), idValidation];
    let labelInput = params.resourceName + ' Id';

    return (
        <>
            <span>
                Are you sure you want to delete this {params.resourceName} ?
            </span>
            <br />
            <br />
            <span>
                You are deleting {params.resourceName}{' '}
                <span style={{ fontWeight: 500 }}>{params.id}</span>
            </span>
            <br />
            <span>To proceed enter the {params.resourceName} Id</span>
            <br />
            <br />
            <span style={{ color: 'red' }}>
                ATTENTION: This operation cannot be undone!
            </span>
            <SimpleForm toolbar={false} mode="onChange" reValidateMode="onBlur">
                <TextInput
                    label={labelInput}
                    source="selectedId"
                    validate={validateClientId}
                    fullWidth
                />
            </SimpleForm>
        </>
    );
};
